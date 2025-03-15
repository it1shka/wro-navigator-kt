package com.it1shka.wronavigatorkt

import com.it1shka.wronavigatorkt.data.DataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WroNavigatorKtApplication @Autowired constructor (
  private val dataService: DataService,
) : CommandLineRunner {
  override fun run(vararg args: String?) {
    val firstStop = dataService.busStops.values.first()
    for (connection in firstStop.connections) {
      println(connection.distance)
    }
  }
}

fun main(args: Array<String>) {
  runApplication<WroNavigatorKtApplication>(*args)
}
