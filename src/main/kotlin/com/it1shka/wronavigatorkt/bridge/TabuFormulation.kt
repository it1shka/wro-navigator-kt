package com.it1shka.wronavigatorkt.bridge

import com.it1shka.wronavigatorkt.algorithm.Solution

enum class AspirationType {
  AVERAGE,
  MAX,
  NONE,
}

enum class SamplingType {
  BY_DISTANCE,
  BY_OVERLAP,
  NONE,
}

data class TabuFormulation (
  val stops: List<String>,
  val time: Int,
  val parameter: Parameter,
  val aspiration: AspirationType,
  val tabuLimit: Boolean,
  val sampling: SamplingType,
  val onChange: ((RoutePlan, RoutePlan, Double, Double) -> Unit)?
)

typealias Route = Solution<StatefulNode>
data class RouteParams (
  val start: String,
  val end: String,
  val time: Int,
  val parameter: Parameter
)

typealias RoutePlan = List<String>
data class RoutePlanWithConfig (
  val plan: RoutePlan,
  val time: Int,
  val parameter: Parameter,
)