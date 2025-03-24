package com.it1shka.wronavigatorkt.data

import com.it1shka.wronavigatorkt.utils.haversine
import com.it1shka.wronavigatorkt.utils.timeDistance
import com.it1shka.wronavigatorkt.utils.toDistanceDescription
import com.it1shka.wronavigatorkt.utils.toTimeDescription
import com.it1shka.wronavigatorkt.utils.toTimeString
import kotlin.math.roundToInt

/**
 * Generic interface for connection between two
 * bus stops. IConnection subclasses must be
 * used after initialization of all stops
 */
interface IConnection {
  val start: BusStop
  val end: BusStop
  val duration: Int
  val distance: Double
  val description: String
  fun waitingTime(time: Int): Int
  fun distanceBetweenStops(): Double {
    val startLocation = start.location
    val endLocation = end.location
    return haversine(startLocation, endLocation)
  }
  fun totalTimeCost(passengerTime: Int): Int {
    return waitingTime(passengerTime) + duration
  }
}

/**
 * Represents connection by bus
 */
data class TransferConnection (
  override val start: BusStop,
  override val end: BusStop,
  val company: String,
  val line: String,
  val departureTime: Int,
  val arrivalTime: Int,
  val startLocation: Pair<Double, Double>,
  val endLocation: Pair<Double, Double>,
) : IConnection {
  override val duration = timeDistance(departureTime, arrivalTime)

  override val distance by lazy {
    haversine(startLocation, endLocation)
  }

  override val description by lazy {
    val startStopName = start.name
    val endStopName = end.name
    val departure = departureTime.toTimeString()
    val arrival = arrivalTime.toTimeString()
    val output = "$company by vehicle \"$line\": $startStopName ($departure) - $endStopName ($arrival)"
    return@lazy output
  }

  override fun waitingTime(passengerTime: Int): Int =
    timeDistance(passengerTime, departureTime)
}

/**
 * Represents direct connection
 * between two stops
 */
data class WalkConnection (
  override val start: BusStop,
  override val end: BusStop,
) : IConnection {
  companion object {
    private const val WALK_SPEED: Double = 5.0
  }

  override val duration by lazy {
    val hours = distance / WALK_SPEED
    val output = (hours * 3600).roundToInt()
    return@lazy output
  }

  override val distance by lazy {
    distanceBetweenStops()
  }

  override val description by lazy {
    val startStopName = start.name
    val endStopName = end.name
    val timeDescription = duration.toTimeDescription()
    val distanceDescription = distance.toDistanceDescription()
    val output = "Walk $distanceDescription from $startStopName to $endStopName for $timeDescription"
    return@lazy output
  }

  override fun waitingTime(passengerTime: Int): Int = 0
}