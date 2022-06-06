package com.github.rainmanhhh.eurekagateway.auth

import org.springframework.stereotype.Component

@Component
class AuthFilterConfig {
  /**
   * filter order
   */
  var order = 200

  /**
   * if no jwt token in [org.springframework.http.HttpHeaders.AUTHORIZATION], try to parse token in cookie with this name
   */
  var cookieName = "JWT"
}
