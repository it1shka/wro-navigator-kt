package com.it1shka.wronavigatorkt.bridge

import com.it1shka.wronavigatorkt.algorithm.Edge
import com.it1shka.wronavigatorkt.algorithm.Problem
import com.it1shka.wronavigatorkt.algorithm.Solution
import com.it1shka.wronavigatorkt.algorithm.dijkstra
import com.it1shka.wronavigatorkt.algorithm.pathFinder
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
      Heuristic.DISTANCE -> haversine(thisStop.location, endStop.location)
      Heuristic.LINES_COUNT -> thisStop.lines.size.toDouble()
      Heuristic.CONNECTION_COUNT -> thisStop.connections.size.toDouble()
      Heuristic.LINES_OVERLAP -> endStop.lines.intersect(thisStop.lines).size.toDouble()
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
        is WalkConnection if (conn.duration > allowedWalkTime) -> Int.MAX_VALUE
        is TransferConnection if (conn.waitingTime(node.time) > allowedWaitTime) -> Int.MAX_VALUE
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
}