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
    for (record in dataService.records.slice(0..5)) {
      println(record)
    }
  }
}

fun main(args: Array<String>) {
  runApplication<WroNavigatorKtApplication>(*args)
}
