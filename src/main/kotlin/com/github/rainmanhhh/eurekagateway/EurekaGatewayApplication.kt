package com.github.rainmanhhh.eurekagateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@EnableCaching
@SpringBootApplication
class EurekaGatewayApplication

fun main(args: Array<String>) {
  runApplication<EurekaGatewayApplication>(*args)
}
