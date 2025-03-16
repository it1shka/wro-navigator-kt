package com.it1shka.wronavigatorkt.data

import com.it1shka.wronavigatorkt.utils.toTimeValue

/**
 * Mutable class representing a bus stop
 */
class BusStop(
  val name: String,
  val connections: MutableList<IConnection> = mutableListOf(),
  val locations: MutableSet<Pair<Double, Double>> = mutableSetOf(),
  val lines: MutableSet<String> = mutableSetOf(),
) {
  // call this only after initialization
  val location by lazy { computeLocation() }

  private fun computeLocation(): Pair<Double, Double> {
    var latitude = 0.0
    var longitude = 0.0
    for ((lat, lon) in locations) {
      latitude += lat
      longitude += lon
    }
    latitude /= locations.count()
    longitude /= locations.count()
    return latitude to longitude
  }

  fun availableConnections(time: Int, interval: Int): List<IConnection> =
    connections.filter { it.waitingTime(time) <= interval }

  fun availableConnections(time: String, interval: String): List<IConnection> {
    val timeValue = time.toTimeValue() ?: return emptyList()
    val intervalValue = interval.toTimeValue() ?: return emptyList()
    return availableConnections(timeValue, intervalValue)
  }
}