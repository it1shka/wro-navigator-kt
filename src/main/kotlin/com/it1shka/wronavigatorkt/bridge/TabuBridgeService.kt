package com.it1shka.wronavigatorkt.bridge

import com.it1shka.wronavigatorkt.algorithm.Solution
import com.it1shka.wronavigatorkt.algorithm.TabuSearch
import com.it1shka.wronavigatorkt.utils.swap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.time.Duration
import kotlin.time.measureTime

private typealias Route = Solution<StatefulNode>
private data class RouteParams (
  val start: String,
  val end: String,
  val time: Int,
  val parameter: Parameter
)
private typealias RoutePlan = List<String>
private data class RoutePlanWithConfig (
  val plan: RoutePlan,
  val time: Int,
  val parameter: Parameter,
)

/**
 * Bridge for Tabu Search.
 * Since Tabu Search is a massive algorithm,
 * we will place the implementation for it
 * in this separate service
 */
@Service
class TabuBridgeService @Autowired constructor (
  private val bridgeService: BridgeService,
  @Value("\${tabu-bridge.max-iterations}")
  private val maxIterations: Int,
  @Value("\${tabu-bridge.memory-size}")
  private val memorySize: Int,
) {
  private val routesCache = hashMapOf<RoutePlanWithConfig, Route>()
  private val routeFragmentsCache = hashMapOf<RouteParams, Route>()

  fun solve(
    stops: List<String>,
    time: Int,
    parameter: Parameter,
  ): Pair<Route, Duration> {
    val searchInstance = TabuSearch (
      initial = stops,
      neighborhood = ::getNeighborhood,
      cost = {evaluateRoutePlan(RoutePlanWithConfig(
        plan = it,
        time = time,
        parameter = parameter,
      ))},
      maxIterations = maxIterations,
      memorySize = memorySize,
    )
    lateinit var solution: Route
    val duration = measureTime {
      solution = planToRoute(RoutePlanWithConfig(
        plan = searchInstance.run(),
        time = time,
        parameter = parameter
      ))
    }
    return solution to duration
  }

  // TODO: starting stop is fixed
  // TODO: report
  // TODO: aspiration and sampling

  private fun evaluateRoutePlan(planConfig: RoutePlanWithConfig): Double {
    val route = planToRoute(planConfig)
    return when (planConfig.parameter) {
      Parameter.TIME -> route.last().end.time - route.first().end.time
      Parameter.TRANSFERS -> route.size
    }.toDouble()
  }

  private fun getNeighborhood(plan: RoutePlan) = iterator {
    for (i in plan.indices) {
      for (j in i + 1 until plan.size) {
        yield(plan.swap(i, j))
      }
    }
  }.asSequence()

  private fun planToRoute(planConfig: RoutePlanWithConfig): Route {
    val cached = routesCache[planConfig]
    if (cached != null) return cached
    var currentTime = planConfig.time
    val routes = (planConfig.plan + planConfig.plan.first())
      .zipWithNext()
      .map { (start, end) ->
        val route = getRouteBetween(RouteParams(start, end, currentTime, planConfig.parameter))
        currentTime = route.last().end.time
        route
      }
    val route = routes.flatten()
    routesCache[planConfig] = route
    return route
  }

  /**
   * I am using the best configuration for
   * the route between two stops:
   * A* with Distance + Overlap
   */
  private fun getRouteBetween(params: RouteParams): Route {
    val cached = routeFragmentsCache[params]
    if (cached != null) return cached
    val formulation = Formulation(
      parameter = params.parameter,
      algorithm = Algorithm.PATHFINDER,
      heuristic = Heuristic.DISTANCE_AND_OVERLAP,
      start = params.start,
      end = params.end,
      time = params.time,
    )
    val (route, _) = bridgeService.solve(formulation)
    routeFragmentsCache[params] = route
    return route
  }
}