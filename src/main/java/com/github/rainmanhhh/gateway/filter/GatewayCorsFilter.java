package com.github.rainmanhhh.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import reactor.core.publisher.Mono;

@ConfigurationProperties("gateway.cors")
@Component
public class GatewayCorsFilter extends CorsConfiguration implements WebFilter, Ordered {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final String PROCESS_FLAG = "__FILTER_FLAG_" + getClass().getName();
  private final Environment environment;
  private Set<String> allowedOriginSet = new HashSet<>(0);
  @Nullable
  private String allowedHeaderStr = null;
  @Nullable
  private String allowedMethodStr = null;
  private String allowCredentialStr = "false";
  @Nullable
  private String exposedHeaderStr = null;
  private boolean enabled = true;
  private int order = -10000;

  public GatewayCorsFilter(Environment environment) {
    this.environment = environment;
  }

  @PostConstruct
  private void init() {
    Set<String> activeProfiles = new HashSet<>(Arrays.asList(environment.getActiveProfiles()));
    applyPermitDefaultValues();
    List<String> allowedOrigins = getAllowedOrigins();
    if (allowedOrigins != null) {
      if ((activeProfiles.contains("production") || activeProfiles.contains("prod"))
        && allowedOrigins.contains("*"))
        log.warn("Using allowedOrigins:* in production is dangerous!");
      this.allowedOriginSet.addAll(allowedOrigins);
    }
    List<String> allowedHeaders = getAllowedHeaders();
    if (allowedHeaders != null) {
      if (allowedHeaders.contains("*")) this.allowedHeaderStr = "*";
      else this.allowedHeaderStr = String.join(",", allowedHeaders);
    }
    List<String> allowedMethods = getAllowedMethods();
    if (allowedMethods != null) this.allowedMethodStr = String.join(",", allowedMethods);
    Boolean allowCredentials = getAllowCredentials();
    if (allowCredentials != null) this.allowCredentialStr = allowCredentials.toString();
    List<String> exposedHeaders = getExposedHeaders();
    if (exposedHeaders != null) this.exposedHeaderStr = String.join(",", exposedHeaders);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @NonNull
  @Override
  public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
    if (!isEnabled()) return chain.filter(exchange);
    Object flag = exchange.getAttribute(PROCESS_FLAG);
    if (flag == null) {
      exchange.getAttributes().put(PROCESS_FLAG, true);
      ServerHttpRequest request = exchange.getRequest();
      ServerHttpResponse response = exchange.getResponse();
      HttpHeaders responseHeaders = response.getHeaders();
      String requestOrigin = request.getHeaders().getOrigin();
      if (requestOrigin != null &&
        (allowedOriginSet.contains("*") || allowedOriginSet.contains(requestOrigin))
      ) {
        responseHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
        if (allowedHeaderStr != null) {
          if (allowedHeaderStr.equals("*")) {
            String allowedHeaders = String.join(",", request.getHeaders().getAccessControlRequestHeaders());
            responseHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders);
          } else {
            responseHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaderStr);
          }
        }
        if (allowedMethodStr != null)
          responseHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowedMethodStr);
        if (allowCredentialStr != null)
          responseHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, allowCredentialStr);
        if (exposedHeaderStr != null)
          responseHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeaderStr);
        Long maxAge = getMaxAge();
        if (maxAge != null)
          responseHeaders.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, maxAge.toString());
        if (request.getMethod() == HttpMethod.OPTIONS) {
          response.setStatusCode(HttpStatus.OK);
          responseHeaders.set(HttpHeaders.ALLOW, "*");
          return Mono.empty();
        }
      }
      return chain.filter(exchange);
    } else return chain.filter(exchange);
  }
}
