package com.it1shka.wronavigatorkt.bridge

import com.it1shka.wronavigatorkt.algorithm.TabuSearch
import com.it1shka.wronavigatorkt.data.BusStop
import com.it1shka.wronavigatorkt.data.DataService
import com.it1shka.wronavigatorkt.utils.haversine
import com.it1shka.wronavigatorkt.utils.swap
import com.it1shka.wronavigatorkt.utils.toTimeDescription
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Bridge for Tabu Search.
 * Since Tabu Search is a massive algorithm,
 * we will place the implementation for it
 * in this separate service
 */
@Service
class TabuBridgeService @Autowired constructor (
  private val bridgeService: BridgeService,
  private val dataService: DataService,
  @Value("\${tabu-bridge.max-iterations}")
  private val maxIterations: Int,
  @Value("\${tabu-bridge.memory-size}")
  private val memorySize: Int,
  @Value("\${tabu-bridge.repetitions-limit}")
  private val repetitionsLimit: Int,
  @Value("\${tabu-bridge.min-sampling}")
  private val minSampling: Int,
) {
  private val routesCache = hashMapOf<RoutePlanWithConfig, Route>()
  private val routeFragmentsCache = hashMapOf<RouteParams, Route>()

  fun solve(formulation: TabuFormulation): Pair<Route, Duration> {
    val searchInstance = TabuSearch (
      initial = formulation.stops,
      neighborhood = applySampling(formulation.sampling),
      cost = {evaluateRoutePlan(RoutePlanWithConfig(
        plan = it,
        time = formulation.time,
        parameter = formulation.parameter,
      ))},
      maxIterations = maxIterations,
      memorySize = memorySize,
      tabuLimit = if (formulation.tabuLimit) formulation.stops.size else null,
      aspiration = when (formulation.aspiration) {
        AspirationType.AVERAGE -> ::aspirationAverage
        AspirationType.MAX -> ::aspirationMax
        AspirationType.NONE -> null
      },
      onChange = formulation.onChange,
      repetitionsLimit = repetitionsLimit,
    )
    lateinit var solution: Route
    val duration = measureTime {
      solution = planToRoute(RoutePlanWithConfig(
        plan = searchInstance.run(),
        time = formulation.time,
        parameter = formulation.parameter
      ))
    }
    return solution to duration
  }

  fun solveAndReport(formulation: TabuFormulation): String {
    val (route, duration) = solve(formulation)
    val mainDescription = route.joinToString("\n") { it.description }
    val timeWeight = (route.last().end.time - route.first().end.time).toTimeDescription()
    val transfersWeight = route.size
    return listOf(
      mainDescription,
      "Time: $timeWeight",
      "Transfers: $transfersWeight",
      "Completed in $duration"
    ).joinToString("\n")
  }

  private fun aspirationAverage(memory: List<Double>, target: Double): Boolean {
    val avg = memory.sum() / memory.size.toDouble()
    return target > avg
  }

  private fun aspirationMax(memory: List<Double>, target: Double): Boolean {
    val max = memory.max()
    return target > max
  }

  private fun evaluateRoutePlan(planConfig: RoutePlanWithConfig): Double {
    val route = planToRoute(planConfig)
    return when (planConfig.parameter) {
      Parameter.TIME -> route.last().end.time - route.first().end.time
      Parameter.TRANSFERS -> route.size
    }.toDouble()
  }

  private fun getNeighborhood(plan: RoutePlan) = iterator {
    for (i in 1 until plan.size) {
      for (j in i + 1 until plan.size) {
        yield(plan.swap(i, j))
      }
    }
  }.asSequence()

  private fun applySampling(sampling: SamplingType): ((RoutePlan) -> Sequence<RoutePlan>) = {
    val neighborhood = getNeighborhood(it)
    when (sampling) {
      SamplingType.BY_DISTANCE -> sampleBy(neighborhood) { start, end ->
        haversine(start.location, end.location)
      }
      SamplingType.BY_OVERLAP -> sampleBy(neighborhood) { start, end ->
        1 / (start.lines.intersect(end.lines).size.toDouble() + 1.0)
      }
      SamplingType.NONE -> neighborhood
    }
  }

  private fun sampleBy(neighborhood: Sequence<RoutePlan>, estimation: (BusStop, BusStop) -> Double): Sequence<RoutePlan> {
    val collectedNeighborhood = neighborhood.toList()
    if (collectedNeighborhood.size <= minSampling) {
      return collectedNeighborhood.asSequence()
    }
    val withDistances = collectedNeighborhood.map { plan ->
      val totalDistance = (plan + plan.first())
        .zipWithNext()
        .sumOf { (start, end) ->
          val startStop = dataService.busStops[start] ?: return@sumOf Double.MAX_VALUE
          val endStop = dataService.busStops[end] ?: return@sumOf Double.MAX_VALUE
          estimation(startStop, endStop)
        }
      plan to totalDistance
    }
    val meanDistance = withDistances.sumOf { it.second } / withDistances.size.toDouble()
    return withDistances
      .asSequence()
      .filter { it.second <= meanDistance }
      .map { it.first }
  }

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