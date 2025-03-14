package com.it1shka.wronavigatorkt.data

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.time.measureTime

// TODO: implement search of stops

@Service
class DataService (
  @Value("classpath:original_graph.csv")
  private val scheduleResource: Resource,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  private val _busStops = mutableMapOf<String, BusStop>()
  val busStops: Map<String, BusStop>
    get() = _busStops.toMap()

  @PostConstruct
  private fun initializeBusStops() {
    val reader = BufferedReader(InputStreamReader(scheduleResource.inputStream))
    var counter = 0
    val initializationTime = measureTime {
      val records = reader
        .lineSequence()
        .map { ScheduleRecord.parseFromLine(it) }
        .filterNotNull()
      records.forEach { record ->
        processScheduleRecord(record)
        counter++
        if (counter % 10_000 == 0) {
          logger.info("Parsed $counter records")
        }
      }
    }
    logger.info("Parsed $counter schedule records in $initializationTime")
  }

  private fun processScheduleRecord(record: ScheduleRecord) {
    if (!_busStops.containsKey(record.startStop)) {
      newBusStop(record.startStop)
    }
    if (!_busStops.containsKey(record.endStop)) {
      newBusStop(record.endStop)
    }
    var ok: Boolean = updateBusStop(record.startStop, record, isStart = true)
    if (!ok) {
      logger.error("Failed to update the start bus stop $record.startStop")
    }
    ok = updateBusStop(record.endStop, record, isStart = false)
    if (!ok) {
      logger.error("Failed to update the end bus stop $record.endStop")
    }
  }

  private fun newBusStop(name: String) {
    _busStops[name] = BusStop(
      name = name,
      connections = listOf(),
      locations = listOf(),
      busLines = listOf()
    )
  }

  private fun updateBusStop(name: String, record: ScheduleRecord, isStart: Boolean): Boolean {
    val previous = _busStops[name] ?: return false
    if (isStart) {
      val endStop = _busStops[record.endStop] ?: return false
      val newConnection = TransferConnection(
        start = previous,
        end = endStop,
        company = record.company,
        line = record.line,
        departureTime = record.departureTime,
        arrivalTime = record.arrivalTime,
      )
      val newLocation = (record.startStopLat to record.startStopLon)
      _busStops[name] = previous.copy(
        connections = previous.connections + newConnection,
        locations = previous.locations + newLocation,
        busLines = previous.busLines + record.line,
      )
      return true
    }
    val newLocation = (record.endStopLat to record.endStopLon)
    _busStops[name] = previous.copy(
      locations = previous.locations + newLocation,
    )
    return true
  }
}