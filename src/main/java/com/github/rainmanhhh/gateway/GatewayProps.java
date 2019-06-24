package com.github.rainmanhhh.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

@ConfigurationProperties("gateway")
@Component
public class GatewayProps {
    @NestedConfigurationProperty
    private CorsConfiguration cors;

    public CorsConfiguration getCors() {
        return cors;
    }

    public GatewayProps setCors(CorsConfiguration cors) {
        this.cors = cors;
        return this;
    }
}
