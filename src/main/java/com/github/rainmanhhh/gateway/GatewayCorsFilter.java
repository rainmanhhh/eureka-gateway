package com.github.rainmanhhh.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import reactor.core.publisher.Mono;

@Component
public class GatewayCorsFilter implements WebFilter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private Set<String> allowedOrigins = new HashSet<>(0);
    private String allowedHeaders = null;
    private String allowedMethods = null;
    private String allowCredentials = "false";
    private String exposedHeaders = null;
    private String maxAge = null;

    public GatewayCorsFilter(GatewayProps gatewayProps, Environment environment) {
        Set<String> activeProfiles = Set.of(environment.getActiveProfiles());
        CorsConfiguration corsProps = gatewayProps.getCors();
        if (corsProps == null) corsProps = new CorsConfiguration();
        corsProps.applyPermitDefaultValues();
        List<String> allowedOrigins = corsProps.getAllowedOrigins();
        if (allowedOrigins != null) {
            if ((activeProfiles.contains("production") || activeProfiles.contains("prod"))
                    && allowedOrigins.contains("*"))
                log.warn("Using allowedOrigins:* in production is dangerous!");
            this.allowedOrigins.addAll(allowedOrigins);
        }
        List<String> allowedHeaders = corsProps.getAllowedHeaders();
        if (allowedHeaders != null) this.allowedHeaders = String.join(",", allowedHeaders);
        List<String> allowedMethods = corsProps.getAllowedMethods();
        if (allowedMethods != null) this.allowedMethods = String.join(",", allowedMethods);
        Boolean allowCredentials = corsProps.getAllowCredentials();
        if (allowCredentials != null) this.allowCredentials = allowCredentials.toString();
        List<String> exposedHeaders = corsProps.getExposedHeaders();
        if (exposedHeaders != null) this.exposedHeaders = String.join(",", exposedHeaders);
        Long maxAge = corsProps.getMaxAge();
        if (maxAge != null) this.maxAge = maxAge.toString();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Object flag = exchange.getAttribute(getClass().getName());
        if (flag == null) {
            exchange.getAttributes().put(getClass().getName(), true);
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders responseHeaders = response.getHeaders();
            if (request.getMethod() == HttpMethod.OPTIONS) {
                response.setStatusCode(HttpStatus.OK);
                responseHeaders.set(HttpHeaders.ALLOW, "*");
                return Mono.empty();
            } else {
                String requestOrigin = request.getHeaders().getOrigin();
                if (requestOrigin != null &&
                        (allowedOrigins.contains("*") || allowedOrigins.contains(requestOrigin))
                ) {
                    responseHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
                    if (allowedHeaders != null)
                        responseHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders);
                    if (allowedMethods != null)
                        responseHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowedMethods);
                    if (allowCredentials != null)
                        responseHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, allowCredentials);
                    if (exposedHeaders != null)
                        responseHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeaders);
                    if (maxAge != null)
                        responseHeaders.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, maxAge);
                }
                return chain.filter(exchange);
            }
        } else return chain.filter(exchange);
    }
}
