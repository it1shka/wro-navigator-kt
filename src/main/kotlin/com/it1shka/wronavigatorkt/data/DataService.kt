package com.it1shka.wronavigatorkt.data

import com.it1shka.wronavigatorkt.utils.haversine
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
  @Value("\${graph.walk-connections.apply-restrictions}")
  private val applyWalkRestrictions: Boolean,
  @Value("\${graph.walk-connections.max-allowed-distance}")
  private val maxWalkAllowedDistance: Double,
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
    val walkJoinTime = measureTime {
      connectByWalk()
    }
    logger.info("Joined by walk stops in $walkJoinTime")
    reportGraph()
  }

  /**
   * Shows amount of nodes
   * and connections between them
   */
  private fun reportGraph() {
    val nodesCount = _busStops.keys.size
    logger.info("Graph contains $nodesCount nodes")
    val connCount = _busStops.values.sumOf { it.connections.size }
    logger.info("Graph contains $connCount edges")
  }

  private fun processScheduleRecord(record: ScheduleRecord) {
    if (!_busStops.containsKey(record.startStop)) {
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
    if (!isStart) {
      busStop.locations.add(record.endStopLat to record.endStopLon)
      return true
    }
    val endStop = _busStops[record.endStop] ?: return false
    val newConnection = TransferConnection(
      start = busStop,
      end = endStop,
      company = record.company,
      line = record.line,
      departureTime = record.departureTime,
      arrivalTime = record.arrivalTime,
      startLocation = record.startStopLat to record.startStopLon,
      endLocation = record.endStopLat to record.endStopLon,
    )
    busStop.connections.add(newConnection)
    busStop.locations.add(record.startStopLat to record.startStopLon)
    busStop.lines.add(record.line)
    return true
  }

  /**
   * Adds connections all to all by walk
   */
  private fun connectByWalk() {
    val stops = _busStops.values.toList()
    for (i in 0 until stops.size) {
      for (j in (i + 1) until stops.size) {
        if (applyWalkRestrictions) {
          val distance = haversine(stops[i].location, stops[j].location)
          if (distance > maxWalkAllowedDistance) continue
        }
        val connectionA = WalkConnection(
          start = stops[i],
          end = stops[j],
        )
        stops[i].connections.add(connectionA)
        val connectionB = WalkConnection(
          start = stops[j],
          end = stops[i],
        )
        stops[j].connections.add(connectionB)
      }
    }
  }
}