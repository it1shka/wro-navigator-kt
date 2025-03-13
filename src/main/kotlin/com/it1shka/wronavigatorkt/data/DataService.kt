package com.it1shka.wronavigatorkt.data

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.time.measureTime

@Service
class DataService (
  @Value("classpath:original_graph.csv")
  private val scheduleResource: Resource,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  private lateinit var _records: List<ScheduleRecord>
  val records: List<ScheduleRecord>
    get() = _records

  @PostConstruct
  fun fetchScheduleRecords() {
    val reader = BufferedReader(InputStreamReader(scheduleResource.inputStream))
    val recordsParsingTime = measureTime {
      _records = reader
        .lineSequence()
        .map { ScheduleRecord.parseFromLine(it) }
        .filterNotNull()
        .toList()
    }
    logger.info("Parsed ${_records.size} schedule records in $recordsParsingTime")
  }
}