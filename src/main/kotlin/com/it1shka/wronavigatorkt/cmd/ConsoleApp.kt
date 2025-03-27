package com.it1shka.wronavigatorkt.cmd

import com.it1shka.wronavigatorkt.bridge.Algorithm
import com.it1shka.wronavigatorkt.bridge.AspirationType
import com.it1shka.wronavigatorkt.bridge.BridgeService
import com.it1shka.wronavigatorkt.bridge.Formulation
import com.it1shka.wronavigatorkt.bridge.Heuristic
import com.it1shka.wronavigatorkt.bridge.Parameter
import com.it1shka.wronavigatorkt.bridge.TabuBridgeService
import com.it1shka.wronavigatorkt.bridge.TabuFormulation
import com.it1shka.wronavigatorkt.data.DataService
import com.it1shka.wronavigatorkt.utils.intoSeconds
import com.it1shka.wronavigatorkt.utils.stringSearch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service

private enum class ProblemType {
  PATH_FINDING,
  TRAVELLING_AROUND,
  GENERATE_PATH,
  EXIT
}

@Service
class ConsoleApp @Autowired constructor(
  private val dataService: DataService,
  private val bridgeService: BridgeService,
  private val tabuBridgeService: TabuBridgeService,
  @Value("\${search.allowed-lexical-distance}")
  private val allowedLexicalDistance: Int,
  @Value("\${general.solution-timeout}")
  private val solutionTimeout: Long,
) : CommandLineRunner {
  private val problemTypes = listOf(
    "Path between two stops" to ProblemType.PATH_FINDING,
    "Circle path between {n} stops" to ProblemType.TRAVELLING_AROUND,
    "Generate long path of stops" to ProblemType.GENERATE_PATH,
    "Stop application" to ProblemType.EXIT
  )

  private val parameters = listOf(
    "Time" to Parameter.TIME,
    "Transfers" to Parameter.TRANSFERS,
  )

  private val heuristics = listOf(
    "Empty" to Heuristic.EMPTY,
    "Distance" to Heuristic.DISTANCE,
    "Lines Count" to Heuristic.LINES_COUNT,
    "Lines Overlap" to Heuristic.LINES_OVERLAP,
    "Connections Count" to Heuristic.CONNECTION_COUNT,
    "Locations Coverage" to Heuristic.LOCATIONS_COVERAGE,
    "Line Popularity" to Heuristic.LINE_POPULARITY,
    "Line Average Time" to Heuristic.LINE_AVG_TIME,
    "Line Average Distance" to Heuristic.LINE_AVG_DISTANCE,
    "Distance + Overlap" to Heuristic.DISTANCE_AND_OVERLAP,
    "Compound Count" to Heuristic.COMPOUND_COUNT
  )

  private val algorithms = listOf(
    "dijkstra" to Algorithm.DIJKSTRA,
    "a*" to Algorithm.PATHFINDER,
  )

  private val aspirationTypes = listOf(
    "Aspiration S' > avg(A)" to AspirationType.AVERAGE,
    "Aspiration S' > max(A)" to AspirationType.MAX,
    "None" to AspirationType.NONE,
  )

  private val tabuLimits = listOf(
    "Unbound Tabu set" to false,
    "Limit Tabu set" to true,
  )

  override fun run(vararg args: String?) = mainLoop()

  private tailrec fun mainLoop() {
    val userChoice = promptFromList(problemTypes, "What would you like to resolve?")
    when (userChoice) {
      ProblemType.EXIT -> return
      ProblemType.PATH_FINDING -> resolvePathFindingProblem()
      ProblemType.TRAVELLING_AROUND -> resolveTravellingAroundProblem()
      ProblemType.GENERATE_PATH -> generateStopPath()
    }
    println("*".repeat(5))
    println()
    return mainLoop()
  }

  private fun generateStopPath() {
    val pathSize = promptPositiveInteger("Please, provide path size: ")
    val busStops = dataService.busStops.keys.shuffled().take(pathSize)
    val path = busStops.joinToString(", ")
    println(path)
  }

  private fun resolveTravellingAroundProblem() {
    val stops = promptListOfStops("Stops: ")
    val startTime = promptTime("Please, enter your starting time: ")
    val parameter = promptFromList(parameters, "Please, enter your starting parameter: ")
    val aspiration = promptFromList(aspirationTypes, "Please, provide the aspiration criteria")
    val tabuLimit = promptFromList(tabuLimits, "Do you want to limit the tabu search? ")
    val report = tabuBridgeService.solveAndReport(TabuFormulation(
      stops = stops,
      time = startTime,
      parameter = parameter,
      aspiration = aspiration,
      tabuLimit = tabuLimit,
      onChange = { prev, next, prevCost, nextCost ->
        println("Optimized $prevCost -> $nextCost: $next")
      }
    ))
    println(report)
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

    // TODO: temporary solution
    // TODO: against OOM errors
    // TODO: after timeout thread is still working
    val report = runBlocking {
      withTimeoutOrNull(solutionTimeout) {
        val busyJob = GlobalScope.async {
          bridgeService.solveAndReport(formulation)
        }
        busyJob.await()
      } ?: "Solution timed out"
    }

    println(report)
  }

  private tailrec fun promptPositiveInteger(message: String): Int {
    println(message)
    val input = (readlnOrNull() ?: "")
    val maybeValue = input.toIntOrNull()
    if (maybeValue != null && maybeValue > 0) {
      return maybeValue
    }
    println("Please, provide a positive integer")
    return promptPositiveInteger(message)
  }

  private tailrec fun promptListOfStops(message: String): List<String> {
    println(message)
    val input = (readlnOrNull() ?: "")
    val stopsList = input
      .split(",")
      .map { stopName ->
        promptStop("Please, provide the stop: ", stopName.trim())
      }
    if (stopsList.size >= 2) return stopsList
    println("Stops list should have the minimum length of 2.")
    return promptListOfStops(message)
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
    println("Potential stops for \"$input\": ")
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