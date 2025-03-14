package com.it1shka.wronavigatorkt.data

import com.it1shka.wronavigatorkt.utils.toTimeValue

data class ScheduleRecord(
  val index: Int,
  val company: String,
  val line: String,
  val departureTime: Int,
  val arrivalTime: Int,
  val startStop: String,
  val endStop: String,
  val startStopLat: Double,
  val startStopLon: Double,
  val endStopLat: Double,
  val endStopLon: Double,
) {
  companion object {
    fun parseFromLine(source: String): ScheduleRecord? {
      val fragments = source.split(',')
      if (fragments.size != 11) return null
      val index = fragments[0].toIntOrNull() ?: return null
      val company = fragments[1]
      val line = fragments[2]
      val departureTime = fragments[3].toTimeValue() ?: return null
      val arrivalTime = fragments[4].toTimeValue() ?: return null
      val startStop = fragments[5]
      val endStop = fragments[6]
      val startStopLat = fragments[7].toDoubleOrNull() ?: return null
      val startStopLon = fragments[8].toDoubleOrNull() ?: return null
      val endStopLat = fragments[9].toDoubleOrNull() ?: return null
      val endStopLon = fragments[10].toDoubleOrNull() ?: return null
      return ScheduleRecord(
        index,
        company,
        line,
        departureTime,
        arrivalTime,
        startStop,
        endStop,
        startStopLat,
        startStopLon,
        endStopLat,
        endStopLon
      )
    }
  }
}