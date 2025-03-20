package com.it1shka.wronavigatorkt.cmd

import com.it1shka.wronavigatorkt.bridge.BridgeService
import com.it1shka.wronavigatorkt.data.BusStop
import com.it1shka.wronavigatorkt.data.DataService
import com.it1shka.wronavigatorkt.utils.toTimeValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service

@Service
class ConsoleApp @Autowired constructor(
  private val dataService: DataService,
  private val bridgeService: BridgeService,
  @Value("\${data.acceptable-lexical-difference}")
  val acceptableLexicalDifference: Int,
) : CommandLineRunner {
  override fun run(vararg args: String?) {
    while (true) {
      val startStop = promptUserForBusStop("Please, provide the start stop: ")
      val endStop = promptUserForBusStop("Please, provide the end stop: ")
      val startTime = promptUserForTime("Please, enter your starting time: ")
      val algorithm = (readlnOrNull() ?: "").trim() // TODO: change to a prompt function
      val problem = bridgeService.formulateTimeProblem(
        startStop = startStop.name,
        endStop = endStop.name,
        startTime = startTime,
      )
      if (problem == null) {
        println("Incorrect values provided.")
        continue
      }
      val solution = bridgeService.solveProblem(problem, algorithm)
      println(solution)
    }
  }

  private fun promptUserForBusStop(promptMessage: String): BusStop {
    while (true) {
      println(promptMessage)
      val userInput = (readlnOrNull() ?: "").trim()
      if (userInput.isBlank()) {
        println("No input provided. Please, try again.")
        continue
      }
      val potentialStopsWithDistances = dataService.findStopsByName(userInput)
      val bestStop = potentialStopsWithDistances.minBy { it.second }
      if (bestStop.second <= acceptableLexicalDifference) {
        return bestStop.first
      }
      val potentialStops = potentialStopsWithDistances
        .sortedBy { it.second }
        .map { it.first }
      println("Potential stops: ")
      for ((index, stop) in potentialStops.withIndex()) {
        println("${index + 1} ${stop.name}")
      }
      println("Select (1-${potentialStops.size}) to choose a stop, or -1 to search again: ")
      val userChoice = (readlnOrNull() ?: "-1").trim().toIntOrNull()?.let { it - 1 } ?: -1
      if (userChoice == -1 || userChoice !in potentialStops.indices) {
        continue
      }
      return potentialStops[userChoice]
    }
  }

  private fun promptUserForTime(promptMessage: String): String {
    while (true) {
      println(promptMessage)
      val userInput = (readlnOrNull() ?: "").trim()
      if (userInput.isBlank()) {
        println("No input provided. Please, try again.")
        continue
      }
      if (userInput.toTimeValue() == null) {
        println("Please, use HH:MM:SS format for time.")
        continue
      }
      return userInput
    }
  }
}