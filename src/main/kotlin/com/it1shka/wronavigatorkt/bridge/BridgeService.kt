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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
  @Value("\${bridge.parameter.transfers.allowed-walk-time}")
  private val allowedWalkTime: Int,
  @Value("\${bridge.parameter.transfers.allowed-wait-time}")
  private val allowedWaitTime: Int,
  @Value("\${bridge.parameter.transfers.penalty}")
  private val penalty: Int,
  @Value("\${bridge.heuristic.avg-bus-speed}")
  private val avgBusSpeed: Double,
  @Value("\${bridge.heuristic.avg-interstop-distance}")
  private val avgInterstopDistance: Double,
  @Value("\${bridge.heuristic.lines-count-to-time}")
  private val linesCountToTime: Double,
  @Value("\${bridge.heuristic.lines-count-to-transfers}")
  private val linesCountToTransfers: Double,
  @Value("\${bridge.heuristic.conn-count-to-time}")
  private val connCountToTime: Double,
  @Value("\${bridge.heuristic.conn-count-to-transfers}")
  private val connCountToTransfers: Double,
  @Value("\${bridge.heuristic.lines-overlap-to-time}")
  private val linesOverlapToTime: Double,
  @Value("\${bridge.heuristic.lines-overlap-to-transfers}")
  private val linesOverlapToTransfers: Double,
  @Value("\${bridge.heuristic.coverage-to-time}")
  private val coverageToTime: Double,
  @Value("\${bridge.heuristic.coverage-to-transfers}")
  private val coverageToTransfers: Double,
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
      ),
      end = StatefulNode(
        stopName = formulation.end,
        time = -1,
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
    val (route, duration) = solve(formulation)
    val routeDescription = route.joinToString("\n") { it.description }
    val durationDescription = "Finished in $duration"
    return "$routeDescription\n$durationDescription"
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
      // compound heuristics
      Heuristic.DISTANCE_AND_OVERLAP -> distanceHeuristic(thisStop, endStop, formulation) + linesOverlapHeuristic(thisStop, endStop, formulation)
      Heuristic.COMPOUND_COUNT -> linesCountHeuristic(thisStop, formulation) + connCountHeuristic(thisStop, formulation) + locationsCoverageHeuristic(thisStop, formulation)
    }
  }

  private fun getEdgeFetcher(parameter: Parameter): BridgeEdgeFetcher = {
    when (parameter) {
      Parameter.TIME -> fetchEdgesByTime(it)
      Parameter.TRANSFERS -> fetchEdgesByTransfers(it)
    }
  }

  // implementations of fetchers and heuristics

  private fun fetchEdgesByTime(node: StatefulNode): List<Edge<StatefulNode>> {
    val busStop = dataService.busStops[node.stopName] ?: return emptyList()
    return busStop.connections.map { conn ->
      val weight = conn.totalTimeCost(node.time)
      Edge(
        start = node,
        end = StatefulNode(
          stopName = conn.end.name,
          time = node.time + weight,
        ),
        description = conn.description,
        weight = weight,
      )
    }
  }

  private fun fetchEdgesByTransfers(node: StatefulNode): List<Edge<StatefulNode>> {
    val busStop = dataService.busStops[node.stopName] ?: return emptyList()
    return busStop.connections.map { conn ->
      val weight = when (conn) {
        is WalkConnection if (conn.duration > allowedWalkTime) -> penalty
        is TransferConnection if (conn.waitingTime(node.time) > allowedWaitTime) -> penalty
        else -> 1
      }
      Edge(
        start = node,
        end = StatefulNode(
          stopName = conn.end.name,
          time = node.time + conn.totalTimeCost(node.time),
        ),
        description = conn.description,
        weight = weight,
      )
    }
  }

  private fun distanceHeuristic(start: BusStop, end: BusStop, formulation: Formulation): Double {
    val pureDistance = haversine(start.location, end.location)
    return when (formulation.parameter) {
      Parameter.TIME -> (pureDistance / avgBusSpeed) * 3600.0 * 0.5
      Parameter.TRANSFERS -> pureDistance / avgInterstopDistance
    }
  }

  private fun linesCountHeuristic(stop: BusStop, formulation: Formulation): Double {
    val lines = stop.lines.size.toDouble()
    return when (formulation.parameter) {
      Parameter.TIME -> asMaximizing(lines, linesCountToTime)
      Parameter.TRANSFERS -> asMaximizing(lines, linesCountToTransfers)
    }
  }

  private fun connCountHeuristic(stop: BusStop, formulation: Formulation): Double {
    val connections = stop.connections.size.toDouble()
    return when (formulation.parameter) {
      Parameter.TIME -> asMaximizing(connections, connCountToTime)
      Parameter.TRANSFERS -> asMaximizing(connections, connCountToTransfers)
    }
  }

  private fun linesOverlapHeuristic(current: BusStop, end: BusStop, formulation: Formulation): Double {
    val overlap = current.lines.intersect(end.lines).size.toDouble()
    return when (formulation.parameter) {
      Parameter.TIME -> asMaximizing(overlap, linesOverlapToTime)
      Parameter.TRANSFERS -> asMaximizing(overlap, linesOverlapToTransfers)
    }
  }

  private fun locationsCoverageHeuristic(current: BusStop, formulation: Formulation): Double {
    val locations = current.locations.size.toDouble()
    return when (formulation.parameter) {
      Parameter.TIME -> asMaximizing(locations, coverageToTime)
      Parameter.TRANSFERS -> asMaximizing(locations, coverageToTransfers)
    }
  }
}