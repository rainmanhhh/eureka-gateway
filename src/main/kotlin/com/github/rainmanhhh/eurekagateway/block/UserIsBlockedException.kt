package com.github.rainmanhhh.eurekagateway.block

import io.jsonwebtoken.JwtException

class UserIsBlockedException(userId: String): JwtException(userId)