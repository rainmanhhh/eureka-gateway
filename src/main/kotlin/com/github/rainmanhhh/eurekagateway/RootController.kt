package com.github.rainmanhhh.eurekagateway

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/")
@RestController
class RootController {

  @GetMapping("/")
  fun hello() = "hello this is gateway"

}
