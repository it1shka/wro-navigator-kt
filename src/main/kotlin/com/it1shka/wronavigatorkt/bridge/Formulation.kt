package com.it1shka.wronavigatorkt.bridge

enum class Parameter {
  TIME,
  TRANSFERS,
}

enum class Algorithm {
  DIJKSTRA,
  PATHFINDER, // A*
}

enum class Heuristic {
  DISTANCE,
  LINES_OVERLAP,
  LINES_COUNT,
  CONNECTION_COUNT,
}

/**
 * Guarantees that the values provided
 * are correct => formulation should
 * contain only correct values
 */
data class Formulation (
  val parameter: Parameter,
  val algorithm: Algorithm,
  val heuristic: Heuristic,
  val start: String,
  val end: String,
  val time: Int,
)