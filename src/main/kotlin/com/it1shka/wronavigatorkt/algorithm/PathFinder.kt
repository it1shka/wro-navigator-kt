package com.it1shka.wronavigatorkt.algorithm

import java.util.PriorityQueue

private const val SKIP_CLOSED = true

fun <Node> pathFinder(problem: Problem<Node>): Solution<Node> {
  val g = mutableMapOf<Node, Int>()
  val h = mutableMapOf<Node, Double>()
  val f = mutableMapOf<Node, Double>()
  fun getG(node: Node) = g.getOrDefault(node, Int.MAX_VALUE)
  fun getH(node: Node) = h.getOrDefault(node, Double.MAX_VALUE)
  fun getF(node: Node) = f.getOrDefault(node, Double.MAX_VALUE)

  val openList = PriorityQueue<Node>(compareBy { getF(it) })
  val closedList = mutableSetOf<Node>()

  val parents = mutableMapOf<Node, Edge<Node>>()

  g[problem.start] = 0
  h[problem.start] = problem.heuristic(problem.start)
  f[problem.start] = problem.heuristic(problem.start)
  openList.add(problem.start)

  while (openList.isNotEmpty()) {
    val currentNode = openList.poll()
    if (currentNode == problem.end) break
    closedList.add(currentNode)

    for (edge in problem.edges(currentNode)) {
      val next = edge.end

      // another modification of a*
      if (SKIP_CLOSED && next in closedList) continue

      val currentDistance = getG(currentNode) + edge.weight
      if (next !in openList && next !in closedList) {
        val currentHeuristic = problem.heuristic(next)
        g[next] = currentDistance
        h[next] = currentHeuristic
        f[next] = currentDistance + currentHeuristic
        parents[next] = edge
        openList.add(next)
        continue
      }
      if (getG(next) > currentDistance) {
        g[next] = currentDistance
        f[next] = currentDistance + getH(next)
        parents[next] = edge
        if (next in closedList) {
          openList.add(next)
          closedList.remove(next)
        } else { // updating the weight
          openList.remove(next)
          openList.add(next)
        }
      }
    }
  }

  val route = mutableListOf<Edge<Node>>()
  var currentEdge = parents[problem.end]
  while (currentEdge != null) {
    route.add(currentEdge)
    currentEdge = parents[currentEdge.start]
  }
  return route.asReversed()
}