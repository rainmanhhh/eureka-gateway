package com.github.rainmanhhh.eurekagateway.rule

import org.springframework.web.bind.annotation.*

@RequestMapping("\${gateway.admin-path}/rule")
@RestController
class RuleController(
  private val ruleService: RuleService
) {
  @GetMapping
  fun getRules() = ruleService.ruleMap

  @PutMapping
  fun putRules(@RequestBody rules: List<Rule>) = ruleService.updateRules(rules)
}