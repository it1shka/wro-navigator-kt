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
  EMPTY,
  DISTANCE,
  LINES_OVERLAP,
  LINES_COUNT,
  CONNECTION_COUNT,
  LOCATIONS_COVERAGE,
  LINE_POPULARITY,
  LINE_AVG_TIME,
  LINE_AVG_DISTANCE,
  // compound heuristics
  DISTANCE_AND_OVERLAP,
  COMPOUND_COUNT,
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