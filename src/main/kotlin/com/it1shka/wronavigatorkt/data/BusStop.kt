package com.it1shka.wronavigatorkt.data

data class BusStop (
  val name: String,
  val connections: List<IConnection>,
  val locations: List<Pair<Double, Double>>,
  val busLines: List<String>,
) {
  val averageLocation by lazy {
    var latitude = 0.0
    var longitude = 0.0
    for (location in locations) {
      latitude += location.first
      longitude += location.second
    }
    latitude /= locations.count()
    longitude /= locations.count()
    return@lazy latitude to longitude
  }
}