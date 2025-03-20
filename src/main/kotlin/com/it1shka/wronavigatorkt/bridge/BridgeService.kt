package com.it1shka.wronavigatorkt.bridge

import com.it1shka.wronavigatorkt.algorithm.Edge
import com.it1shka.wronavigatorkt.algorithm.Problem
import com.it1shka.wronavigatorkt.algorithm.dijkstra
import com.it1shka.wronavigatorkt.data.DataService
import com.it1shka.wronavigatorkt.utils.toTimeValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.time.measureTime

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
) {
  fun formulateTimeProblem(
    startStop: String,
    endStop: String,
    startTime: String,
  ): Problem<TimeNode>? {
    if (!dataService.busStops.containsKey(startStop) || !dataService.busStops.containsKey(endStop)) {
      return null
    }
    val startMoment = startTime.toTimeValue(applyModulus = true) ?: return null
    return Problem(
      start = TimeNode(
        stopName = startStop,
        time = startMoment,
      ),
      end = TimeNode(
        stopName = endStop,
        time = -1
      ),
      edges = this::fetchEdgesByTime,
    )
  }

  private fun fetchEdgesByTime(node: TimeNode): List<Edge<TimeNode>> {
    val busStop = dataService.busStops[node.stopName] ?: return emptyList()
    return busStop.connections.map { conn ->
      val weight = conn.totalTimeCost(node.time)
      Edge(
        start = node,
        end = TimeNode(
          stopName = conn.end.name,
          time = node.time + weight,
        ),
        description = conn.description,
        weight = weight,
      )
    }
  }

  fun <Node> solveProblem(problem: Problem<Node>, algorithm: String): String {
    var route: List<Edge<Node>>? = null
    val time = measureTime {
      route = when (algorithm.lowercase()) {
        "dijkstra" -> dijkstra(problem)
        else -> null
      }
    }
    return if (route == null) "No such algorithm: $algorithm"
    else compressRoute(route) + "\nFinished in $time"
  }

  private fun <Node> compressRoute(route: List<Edge<Node>>): String {
    // TODO: compress it normally
    return route.joinToString("\n") { it.description }
  }
}