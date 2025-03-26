package com.it1shka.wronavigatorkt.bridge

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class BridgeConfiguration @Autowired constructor (
  @Value("\${bridge.parameter.transfers.allowed-walk-time}")
  val allowedWalkTime: Int,
  @Value("\${bridge.parameter.transfers.allowed-wait-time}")
  val allowedWaitTime: Int,
  @Value("\${bridge.parameter.transfers.penalty}")
  val penalty: Int,
  @Value("\${bridge.heuristic.avg-bus-speed}")
  val avgBusSpeed: Double,
  @Value("\${bridge.heuristic.avg-interstop-distance}")
  val avgInterstopDistance: Double,
  @Value("\${bridge.heuristic.lines-count-to-time}")
  val linesCountToTime: Double,
  @Value("\${bridge.heuristic.lines-count-to-transfers}")
  val linesCountToTransfers: Double,
  @Value("\${bridge.heuristic.conn-count-to-time}")
  val connCountToTime: Double,
  @Value("\${bridge.heuristic.conn-count-to-transfers}")
  val connCountToTransfers: Double,
  @Value("\${bridge.heuristic.lines-overlap-to-time}")
  val linesOverlapToTime: Double,
  @Value("\${bridge.heuristic.lines-overlap-to-transfers}")
  val linesOverlapToTransfers: Double,
  @Value("\${bridge.heuristic.coverage-to-time}")
  val coverageToTime: Double,
  @Value("\${bridge.heuristic.coverage-to-transfers}")
  val coverageToTransfers: Double,
  @Value("\${bridge.heuristic.lines-popularity-to-time}")
  val linesPopularityToTime: Double,
  @Value("\${bridge.heuristic.lines-popularity-to-transfers}")
  val linesPopularityToTransfers: Double,
  @Value("\${bridge.heuristic.lines-avg-time-importance}")
  val linesAvgTimeImportance: Double,
  @Value("\${bridge.heuristic.lines-avg-dist-importance}")
  val linesAvgDistImportance: Double,
  @Value("\${bridge.heuristic.avg-transfer-time}")
  val avgTransferTime: Double,
)