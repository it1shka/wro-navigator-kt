package com.it1shka.wronavigatorkt.data

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

  fun availableConnections(time: Int): List<IConnection> =
    connections.filter { it.available(time) }
}