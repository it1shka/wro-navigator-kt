package com.it1shka.wronavigatorkt.algorithm

fun <Node> traceRoute(footprints: Map<Node, Edge<Node>>, end: Node): Solution<Node> {
  val route = mutableListOf<Edge<Node>>()
  var currentEdge = footprints[end]
  while (currentEdge != null) {
    route.add(currentEdge)
    currentEdge = footprints[currentEdge.start]
  }
  return route.asReversed()
}