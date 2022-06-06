package com.github.rainmanhhh.eurekagateway

import com.github.rainmanhhh.eurekagateway.auth.AuthFilterConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.stereotype.Component

@Suppress("unused")
@ConfigurationProperties("gateway")
@Component
class EurekaGatewayConfig {
  @NestedConfigurationProperty
  lateinit var auth: AuthFilterConfig

  /**
   * root path for gateway admin endpoints. uppercase first letter to make these endpoints follow a seperate rule group
   */
  lateinit var adminPath: String
}