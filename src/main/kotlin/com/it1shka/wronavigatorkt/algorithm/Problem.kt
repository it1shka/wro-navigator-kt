package com.it1shka.wronavigatorkt.algorithm

typealias Heuristic <Node> = (node: Node) -> Double

data class Edge <Node> (
  val start: Node,
  val end: Node,
  val weight: Int,
  val description: String,
)

data class Problem <Node> (
  val start: Node,
  val end: Node,
  val edges: (node: Node) -> List<Edge<Node>>,
  val heuristic: Heuristic<Node>,
)

typealias Solution <Node> = List<Edge<Node>>