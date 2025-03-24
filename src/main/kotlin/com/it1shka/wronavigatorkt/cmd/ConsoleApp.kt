package com.it1shka.wronavigatorkt.cmd

import com.it1shka.wronavigatorkt.bridge.Algorithm
import com.it1shka.wronavigatorkt.bridge.BridgeService
import com.it1shka.wronavigatorkt.bridge.Formulation
import com.it1shka.wronavigatorkt.bridge.Heuristic
import com.it1shka.wronavigatorkt.bridge.Parameter
import com.it1shka.wronavigatorkt.data.DataService
import com.it1shka.wronavigatorkt.utils.intoSeconds
import com.it1shka.wronavigatorkt.utils.stringSearch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service

@Service
class ConsoleApp @Autowired constructor(
  private val dataService: DataService,
  private val bridgeService: BridgeService,
  @Value("\${search.allowed-lexical-distance}")
  val allowedLexicalDistance: Int,
) : CommandLineRunner {
  private val parameters = listOf(
    "time" to Parameter.TIME,
    "transfers" to Parameter.TRANSFERS,
  )
  private val heuristics = listOf(
    "empty" to Heuristic.EMPTY,
    "distance" to Heuristic.DISTANCE,
    "lines-count" to Heuristic.LINES_COUNT,
    "lines-overlap" to Heuristic.LINES_OVERLAP,
    "conn-count" to Heuristic.CONNECTION_COUNT,
    "coverage" to Heuristic.LOCATIONS_COVERAGE,
  )
  private val algorithms = listOf(
    "dijkstra" to Algorithm.DIJKSTRA,
    "a*" to Algorithm.PATHFINDER,
  )

  override fun run(vararg args: String?) {
    mainLoop()
  }

  private tailrec fun mainLoop() {
    resolvePathFindingProblem()
    println("*".repeat(5))
    println()
    return mainLoop()
  }

  private fun resolvePathFindingProblem() {
    val startStop = promptStop("Please, provide the start stop: ")
    val endStop = promptStop("Please, provide the end stop: ")
    val startTime = promptTime("Please, enter your starting time: ")
    val algorithm = promptFromList(algorithms, "Please, enter your algorithm: ")
    val parameter = promptFromList(parameters, "Please, enter optimization parameter: ")
    val heuristic = if (algorithm == Algorithm.DIJKSTRA)
      Heuristic.EMPTY
      else promptFromList(heuristics, "Please, enter your heuristic: ")
    val formulation = Formulation(
      parameter = parameter,
      algorithm = algorithm,
      heuristic = heuristic,
      start = startStop,
      end = endStop,
      time = startTime,
    )
    val solution = bridgeService.solveAndReport(formulation)
    println(solution)
  }

  private tailrec fun <T> promptFromList(list: List<Pair<String, T>>, message: String): T {
    println(message)
    listOptions(list.map { it.first })
    val input = (readlnOrNull() ?: "")
      .trim()
      .toIntOrNull()
      ?.let { it - 1 }
    return when (input) {
      is Int if input in list.indices ->
        list[input].second
      else -> {
        println("Wrong choice.")
        promptFromList(list, message)
      }
    }
  }

  private fun listOptions(options: Iterable<String>) {
    println("Available options: ")
    for ((index, option) in options.withIndex()) {
      println("${index + 1}) $option")
    }
  }

  private tailrec fun promptStop(message: String, previousInput: String? = null): String {
    val input = if (previousInput != null) previousInput else {
      println(message)
      (readlnOrNull() ?: "").trim()
    }
    if (input.isBlank()) {
      println("No input provided. Please, try again.")
      return promptStop(message)
    }
    val (best, bestDistance, otherPossibilities) = stringSearch(input, dataService.busStops.keys)
    if (bestDistance <= allowedLexicalDistance) {
      return best
    }
    println("Potential stops: ")
    for ((index, stop) in otherPossibilities.withIndex()) {
      println("${index + 1} ${stop.first} (diff: ${stop.second})")
    }
    val repeatedInput = (readlnOrNull() ?: "").trim()
    val maybeChoice = repeatedInput.toIntOrNull()?.let { it - 1 }
    if (maybeChoice != null && maybeChoice in otherPossibilities.indices) {
      return otherPossibilities[maybeChoice].first
    }
    return promptStop(message, repeatedInput)
  }

  private tailrec fun promptTime(message: String): Int {
    println(message)
    val input = (readlnOrNull() ?: "").trim()
    if (input.isBlank()) {
      println("No input provided. Please, try again.")
      return promptTime(message)
    }
    val maybeSeconds = input.intoSeconds()
    if (maybeSeconds == null) {
      println("Wrong time format. Please, specify time in the format of HH:MM:SS.")
      return promptTime(message)
    }
    return maybeSeconds
  }
}