package com.it1shka.wronavigatorkt.data

class BusStop (val name: String) {
  val connections: List<IConnection>
    get() = _connections
  private val _connections = mutableListOf<IConnection>()

  val locations: List<Pair<Double, Double>>
    get() = _locations
  private val _locations = mutableListOf<Pair<Double, Double>>()

  val busLines: List<String>
    get() = _busLines
  private val _busLines = mutableListOf<String>()

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

  fun addConnection(connection: IConnection) {
    _connections.add(connection)
  }

  fun addLocation(location: Pair<Double, Double>) {
    _locations.add(location)
  }

  fun addBusLine(line: String) {
    _busLines.add(line)
  }
}