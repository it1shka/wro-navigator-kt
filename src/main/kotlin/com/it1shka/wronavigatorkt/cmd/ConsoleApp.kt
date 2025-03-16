package com.it1shka.wronavigatorkt.cmd

import com.it1shka.wronavigatorkt.data.BusStop
import com.it1shka.wronavigatorkt.data.DataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service

@Service
class ConsoleApp @Autowired constructor(
  private val dataService: DataService,
) : CommandLineRunner {
  override fun run(vararg args: String?) {
    while (true) {
      val startStop = promptUserForBusStop("Please, provide the start stop: ")
      val endStop = promptUserForBusStop("Please, provide the end stop: ")
      // TODO: process the inputs
      println("${startStop.name} -> ${endStop.name}")
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
      val potentialStops = dataService.findStopsByName(userInput)
      println("Potential stops: ")
      for ((index, stop) in potentialStops.withIndex()) {
        println("${index + 1} ${stop.name}")
      }
      println("Select (1-${potentialStops.size}) to choose a stop, or -1 to search again: ")
      val userChoice = (readlnOrNull() ?: "-1").trim().toIntOrNull() ?: -1
      if (userChoice == -1 || userChoice !in potentialStops.indices) {
        continue
      }
      return potentialStops[userChoice]
    }
  }
}