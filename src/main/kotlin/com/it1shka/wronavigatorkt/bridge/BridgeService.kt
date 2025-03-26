package com.it1shka.wronavigatorkt.bridge

import com.it1shka.wronavigatorkt.algorithm.Edge
import com.it1shka.wronavigatorkt.algorithm.Problem
import com.it1shka.wronavigatorkt.algorithm.Solution
import com.it1shka.wronavigatorkt.algorithm.dijkstra
import com.it1shka.wronavigatorkt.algorithm.pathFinder
import com.it1shka.wronavigatorkt.data.BusStop
import com.it1shka.wronavigatorkt.data.DataService
import com.it1shka.wronavigatorkt.data.TransferConnection
import com.it1shka.wronavigatorkt.data.WalkConnection
import com.it1shka.wronavigatorkt.utils.haversine
import com.it1shka.wronavigatorkt.utils.toTimeDescription
import com.it1shka.wronavigatorkt.utils.toTimeString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.time.Duration
import kotlin.time.measureTime

private typealias BridgeAlgorithm = (problem: Problem<StatefulNode>) -> Solution<StatefulNode>
private typealias BridgeHeuristic = (node: StatefulNode) -> Double
private typealias BridgeEdgeFetcher = (node: StatefulNode) -> List<Edge<StatefulNode>>
private typealias BridgeSolution = Solution<StatefulNode>

private fun asMaximizing(mainValue: Double, conversionValue: Double): Double {
  return (1.0 / (mainValue + 1.0)) * conversionValue
}

/**
 * Bridge creates instance of
 * a problem using the existing data
 * and then passes this problem to an algorithm.
 * Wrapper that inserts data into algorithms
 * and runs them
 */
@Service
class BridgeService @Autowired constructor(
  private val dataService: DataService,
  private val config: BridgeConfiguration,
) {
  /**
   * Does not validate the input
   */
  fun solve(formulation: Formulation): Pair<BridgeSolution, Duration> {
    val edgeFetcher = getEdgeFetcher(formulation.parameter)
    val heuristic = getHeuristic(formulation)
    val problem = Problem(
      start = StatefulNode(
        stopName = formulation.start,
        time = formulation.time,
        transfers = 0,
        ridesAndWalks = 0,
        lastLine = null,
      ),
      end = StatefulNode(
        stopName = formulation.end,
        time = -1,
        transfers = -1,
        ridesAndWalks = -1,
        lastLine = null,
      ),
      edges = edgeFetcher,
      heuristic = heuristic,
    )
    val algorithm = getAlgorithm(formulation.algorithm)
    lateinit var route: Solution<StatefulNode>
    val elapsedTime = measureTime {
      route = algorithm(problem)
    }
    return route to elapsedTime
  }

  fun solveAndReport(formulation: Formulation): String {
    val (route, runtime) = solve(formulation)
    if (route.isEmpty()) {
      return "You are already on the right stop :)"
    }

    val routeSummary = route.joinToString("\n") { it.description }

    val routeStartTime = route.first().start.time.toTimeString()
    val routeEndTime = route.last().end.time.toTimeString()
    val routeDuration = (route.last().end.time - route.first().start.time).toTimeDescription()
    val durationSummary = "Time: $routeStartTime - $routeEndTime ($routeDuration)"

    val transfersAmount = route.last().end.transfers
    val transfersSummary = "Transfers: $transfersAmount"

    val linesCount = route.last().end.ridesAndWalks
    val linesSummary = "Different lines and walks: $linesCount"

    val weightSummary = when (formulation.parameter) {
      Parameter.TIME -> "Minimized parameter: Time"
      Parameter.TRANSFERS -> "Minimized parameter: Transfers"
    }

    val runtimeSummary = "Finished in $runtime"
    return listOf(
      routeSummary,
      durationSummary,
      transfersSummary,
      linesSummary,
      weightSummary,
      runtimeSummary,
    ).joinToString("\n")
  }

  private fun getAlgorithm(algorithmType: Algorithm): BridgeAlgorithm = {
    when (algorithmType) {
      Algorithm.DIJKSTRA -> dijkstra(it)
      Algorithm.PATHFINDER -> pathFinder(it)
    }
  }

  private fun getHeuristic(formulation: Formulation): BridgeHeuristic = heuristic@ {
    val thisStop = dataService.busStops[it.stopName] ?: return@heuristic 0.0
    val endStop = dataService.busStops[formulation.end] ?: return@heuristic 0.0
    when (formulation.heuristic) {
      Heuristic.EMPTY -> 0.0
      Heuristic.DISTANCE -> distanceHeuristic(thisStop, endStop, formulation)
      Heuristic.LINES_COUNT -> linesCountHeuristic(thisStop, formulation)
      Heuristic.CONNECTION_COUNT -> connCountHeuristic(thisStop, formulation)
      Heuristic.LINES_OVERLAP -> linesOverlapHeuristic(thisStop, endStop, formulation)
      Heuristic.LOCATIONS_COVERAGE -> locationsCoverageHeuristic(thisStop, formulation)
      Heuristic.LINE_POPULARITY -> linePopularityHeuristic(thisStop, formulation)
      Heuristic.LINE_AVG_TIME -> lineAvgTimeHeuristic(thisStop, formulation)
      Heuristic.LINE_AVG_DISTANCE -> lineAvgDistanceHeuristic(thisStop, formulation)
      // compound heuristics
      Heuristic.DISTANCE_AND_OVERLAP -> distanceHeuristic(thisStop, endStop, formulation) + linesOverlapHeuristic(thisStop, endStop, formulation)
      Heuristic.COMPOUND_COUNT -> linesCountHeuristic(thisStop, formulation) + connCountHeuristic(thisStop, formulation) + locationsCoverageHeuristic(thisStop, formulation)
    }
  }

  private fun getEdgeFetcher(parameter: Parameter): BridgeEdgeFetcher = fetcher@ { node ->
    val busStop = dataService.busStops[node.stopName] ?: return@fetcher emptyList()
    busStop.connections.map { conn ->
      val next = node.evolve(conn)
      val weight = when (parameter) {
        Parameter.TIME -> conn.totalTimeCost(node.time)
        Parameter.TRANSFERS -> when (conn) {
          is WalkConnection if (conn.duration > config.allowedWalkTime) -> config.penalty
          is TransferConnection if (conn.waitingTime(node.time) > config.allowedWaitTime) -> config.penalty
          else -> 1
        }
      }
      Edge(
        start = node,
        end = next,
        description = conn.description,
        weight = weight,
      )
    }
  }

  // implementations of heuristics

  private fun linePopularityHeuristic(stop: BusStop, formulation: Formulation): Double {
    val popularity = stop.lines.sumOf {
      dataService.linePopularity[it]?.toDouble() ?: Double.MAX_VALUE
    } / stop.lines.size.toDouble()
    return when (formulation.parameter) {
      Parameter.TIME -> asMaximizing(popularity, config.linesPopularityToTime)
      Parameter.TRANSFERS -> asMaximizing(popularity, config.linesPopularityToTransfers)
    }
  }

  private fun lineAvgTimeHeuristic(stop: BusStop, formulation: Formulation): Double {
    val averageTime = stop.lines.sumOf {
      dataService.averageTimeOf(it)?.toDouble() ?: Double.MAX_VALUE
    } / stop.lines.size.toDouble()
    return when (formulation.parameter) {
      Parameter.TIME -> averageTime
      Parameter.TRANSFERS -> averageTime / config.avgTransferTime
    } * config.linesAvgTimeImportance
  }

  private fun lineAvgDistanceHeuristic(stop: BusStop, formulation: Formulation): Double {
    val averageDistance = stop.lines.sumOf {
      dataService.averageDistanceOf(it) ?: Double.MAX_VALUE
    } / stop.lines.size.toDouble()
    return when (formulation.parameter) {
      Parameter.TIME -> asMaximizing(averageDistance / config.avgBusSpeed, 1.0)
      Parameter.TRANSFERS -> asMaximizing(averageDistance / config.avgInterstopDistance, 1.0)
    } * config.linesAvgDistImportance
  }

  private fun distanceHeuristic(start: BusStop, end: BusStop, formulation: Formulation): Double {
    val pureDistance = haversine(start.location, end.location)
    return when (formulation.parameter) {
      Parameter.TIME -> (pureDistance / config.avgBusSpeed) * 3600.0 * 0.5
      Parameter.TRANSFERS -> pureDistance / config.avgInterstopDistance
    }
  }

  private fun linesCountHeuristic(stop: BusStop, formulation: Formulation): Double {
    val lines = stop.lines.size.toDouble()
    return when (formulation.parameter) {
      Parameter.TIME -> asMaximizing(lines, config.linesCountToTime)
      Parameter.TRANSFERS -> asMaximizing(lines, config.linesCountToTransfers)
    }
  }

  private fun connCountHeuristic(stop: BusStop, formulation: Formulation): Double {
    val connections = stop.connections.size.toDouble()
    return when (formulation.parameter) {
      Parameter.TIME -> asMaximizing(connections, config.connCountToTime)
      Parameter.TRANSFERS -> asMaximizing(connections, config.connCountToTransfers)
    }
  }

  private fun linesOverlapHeuristic(current: BusStop, end: BusStop, formulation: Formulation): Double {
    val overlap = current.lines.intersect(end.lines).size.toDouble()
    return when (formulation.parameter) {
      Parameter.TIME -> asMaximizing(overlap, config.linesOverlapToTime)
      Parameter.TRANSFERS -> asMaximizing(overlap, config.linesOverlapToTransfers)
    }
  }

  private fun locationsCoverageHeuristic(current: BusStop, formulation: Formulation): Double {
    val locations = current.locations.size.toDouble()
    return when (formulation.parameter) {
      Parameter.TIME -> asMaximizing(locations, config.coverageToTime)
      Parameter.TRANSFERS -> asMaximizing(locations, config.coverageToTransfers)
    }
  }
}