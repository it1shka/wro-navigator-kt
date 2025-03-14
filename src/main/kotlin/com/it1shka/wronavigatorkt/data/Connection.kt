package com.it1shka.wronavigatorkt.data

import com.it1shka.wronavigatorkt.utils.haversine
import com.it1shka.wronavigatorkt.utils.toTimeDescription
import com.it1shka.wronavigatorkt.utils.toTimeString
import kotlin.math.roundToInt

interface IConnection {
  val start: BusStop
  val end: BusStop
  val timeWeight: Int
  val description: String
  fun available(time: Int): Boolean
}

data class TransferConnection (
  override val start: BusStop,
  override val end: BusStop,
  val company: String,
  val line: String,
  val departureTime: Int,
  val arrivalTime: Int,
) : IConnection {
  override val timeWeight = arrivalTime - departureTime
  override val description: String
    get() {
      val startStopName = start.name
      val endStopName = end.name
      val departure = departureTime.toTimeString()
      val arrival = arrivalTime.toTimeString()
      return "$line by bus \"$line\": $startStopName ($departure) - $endStopName $arrival"
    }
  override fun available(time: Int) = time <= departureTime
}

data class WalkConnection (
  override val start: BusStop,
  override val end: BusStop,
) : IConnection {
  companion object {
    private const val WALK_SPEED: Double = 5.0;
  }
  override val timeWeight: Int
    get() {
      val distance = haversine(start.averageLocation, end.averageLocation)
      val hours = distance / WALK_SPEED
      return (hours * 3600).roundToInt()
    }
  override val description: String
    get() {
      val startStopName = start.name
      val endStopName = end.name
      val timeDescription = timeWeight.toTimeDescription()
      return "Walk from $startStopName to $endStopName for $timeDescription"
    }

  override fun available(time: Int) = true
}