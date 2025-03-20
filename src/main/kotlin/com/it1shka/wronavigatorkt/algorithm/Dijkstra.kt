package com.it1shka.wronavigatorkt.algorithm

import java.util.PriorityQueue

/*
Procedure Dijkstra(Graph, source):
    Create an empty priority queue Q
    Create a set dist[] and initialize it with infinity for all nodes except the source, which starts with distance 0
    Enqueue source to Q with distance 0

    while Q is not empty:
        Dequeue the node u from Q
        For each neighbor v of u:
            Calculate a tentative distance d from source to v as dist[u] + weight(u, v)
            If d < dist[v]:
                Update the distance of v to d
                Enqueue v to Q with distance d

    Return dist[]
End Procedure
 */

fun <Node> dijkstra(problem: Problem<Node>): Solution<Node> {
  val distances = mutableMapOf<Node, Int>()
  val footprints = mutableMapOf<Node, Edge<Node>>()
  val queue = PriorityQueue<Pair<Node, Int>>(compareBy { it.second })
  distances[problem.start] = 0
  queue.add(problem.start to 0)
  while (queue.isNotEmpty()) {
    val (node, distance) = queue.poll()
    for (edge in problem.edges(node)) {
      val distance = edge.weight + distance
      val existingDistance = distances.getOrDefault(edge.end, Int.MAX_VALUE)
      if (distance < existingDistance) {
        distances[edge.end] = distance
        footprints[edge.end] = edge
        queue.add(edge.end to distance)
      }
    }
  }
  val route = mutableListOf<Edge<Node>>()
  var currentEdge = footprints[problem.end]
  while (currentEdge != null) {
    route.add(currentEdge)
    currentEdge = footprints[currentEdge.start]
  }
  return route
}