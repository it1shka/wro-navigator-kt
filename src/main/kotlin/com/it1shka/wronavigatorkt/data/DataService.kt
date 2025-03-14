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

  val busStops: Map<String, BusStop>
    get() = _busStops
  private val _busStops = mutableMapOf<String, BusStop>()

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
    if (!busStops.containsKey(record.startStop)) {
      _busStops[record.startStop] = BusStop(record.startStop)
    }
    if (!_busStops.containsKey(record.endStop)) {
      _busStops[record.endStop] = BusStop(record.endStop)
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

  private fun updateBusStop(name: String, record: ScheduleRecord, isStart: Boolean): Boolean {
    val busStop = _busStops[name] ?: return false
    if (isStart) {
      val endStop = _busStops[record.endStop] ?: return false
      val newConnection = TransferConnection(
        start = busStop,
        end = endStop,
        company = record.company,
        line = record.line,
        departureTime = record.departureTime,
        arrivalTime = record.arrivalTime,
      )
      val newLocation = (record.startStopLat to record.startStopLon)
      busStop.addConnection(newConnection)
      busStop.addLocation(newLocation)
      busStop.addBusLine(record.line)
      return true
    }
    val newLocation = (record.endStopLat to record.endStopLon)
    busStop.addLocation(newLocation)
    return true
  }
}