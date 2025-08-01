package ez.gateway.eureka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Component
import org.springframework.util.MimeTypeUtils
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.HtmlUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

@Component
class ErrorResponseFilter : GlobalFilter, Ordered {
  companion object {
    private val logger = LoggerFactory.getLogger(ErrorResponseFilter::class.java)
    private val objectMapper = jacksonObjectMapper()
  }

  val processFlag = "__FILTER_PROCESSED_" + javaClass.name
  override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
  override fun filter(
    exchange: ServerWebExchange,
    chain: GatewayFilterChain
  ): Mono<Void> {
    val flag = exchange.getAttribute<Any>(processFlag)
    return if (flag == null) mono {
      val fixedExchange = beforeChain(exchange)
      if (fixedExchange != null) {
        chain.filter(fixedExchange).awaitFirstOrNull()
        afterChain(fixedExchange)
      }
    }.onErrorStop().then() else chain.filter(exchange)
  }

  suspend fun beforeChain(exchange: ServerWebExchange): ServerWebExchange? {
    val decoratedResponse = object : ServerHttpResponseDecorator(exchange.response) {
      override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
        val self = this
        return super.writeWith(wrapBody(exchange.request, self, body))
      }
    }
    return exchange.mutate().response(decoratedResponse).build()
  }

  private fun wrapBody(
    req: ServerHttpRequest,
    res: ServerHttpResponse,
    body: Publisher<out DataBuffer>
  ): Publisher<out DataBuffer> {
    val statusCode = res.statusCode
    return if (statusCode != null && statusCode.isError) mono {
      val dataBufferFactory: DataBufferFactory = res.bufferFactory()
      val dataBuffers = Flux.from(body).collectList().awaitSingle()
      // 计算所有缓冲区的总长度
      val combinedSize = dataBuffers.sumOf { it.readableByteCount() }
      // 创建一个新的缓冲区来容纳所有数据
      val combinedBuffer = dataBufferFactory.allocateBuffer(combinedSize)
      // 将所有缓冲区的数据复制到新缓冲区中
      for (buffer in dataBuffers) {
        combinedBuffer.write(buffer)
        // 释放原始缓冲区
        DataBufferUtils.release(buffer)
      }
      // 在新的缓冲区基础上进行修改
      var finalBodyStr = combinedBuffer.toString(StandardCharsets.UTF_8)
      var dbMsg = finalBodyStr.substringAfter("jOOQ;", "")
      if (dbMsg.isNotEmpty()) {
        val path = req.path
        val contentType = res.headers.contentType
        finalBodyStr =
          if (contentType != null && contentType.includes(MimeTypeUtils.APPLICATION_JSON)) {
            """{"status":500,"message":"db error","path":"${objectMapper.writeValueAsString(path.toString())}"}"""
          } else {
            dbMsg = HtmlUtils.htmlUnescape(dbMsg.substringBeforeLast("</div></body></html>"))
            "db error. path: [$path]"
          }
        logger.error("db error [{}]: {}", path, dbMsg)
        res.headers.set(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
        res.headers.set(HttpHeaders.CONTENT_LENGTH, finalBodyStr.length.toString())
      }
      dataBufferFactory.wrap(finalBodyStr.toByteArray())
    } else body
  }

  suspend fun afterChain(exchange: ServerWebExchange) {
  }
}