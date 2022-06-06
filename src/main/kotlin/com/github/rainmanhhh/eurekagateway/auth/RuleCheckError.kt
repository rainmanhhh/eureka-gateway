package com.github.rainmanhhh.eurekagateway.auth

import com.github.rainmanhhh.eurekagateway.rule.Rule

class RuleCheckError(rule: Rule) : RuntimeException(rule.group!! + ':' + rule.pattern!!)