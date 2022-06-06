package com.github.rainmanhhh.eurekagateway.rule

import org.springframework.stereotype.Component

@Component
class RuleService {
  @Volatile
  final var ruleMap: Map<String, List<Rule>> = mapOf()
    private set

  fun updateRules(list: Iterable<Rule>) {
    val groupMap = hashMapOf<String, MutableList<Rule>>()
    val commonList = arrayListOf<Rule>()
    for (rule in list) {
      val group = rule.group!!
      if (group == "") commonList.add(rule)
      else {
        val subList = groupMap[group] ?: kotlin.run {
          val newList = arrayListOf<Rule>()
          groupMap[group] = newList
          newList
        }
        subList.add(rule)
      }
    }
    for (value in groupMap.values) {
      value.addAll(commonList)
      value.sortBy { it.priority!! }
    }
    groupMap[""] = commonList.apply { sortBy { it.priority } }
    ruleMap = groupMap
  }
}
