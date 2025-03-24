package com.it1shka.wronavigatorkt.bridge

import com.it1shka.wronavigatorkt.data.IConnection
import com.it1shka.wronavigatorkt.data.TransferConnection

/**
 * Additionally stores passenger time
 */
class StatefulNode (
  val stopName: String,
  val time: Int,
  val transfers: Int,
  val ridesAndWalks: Int,
  val lastLine: String?
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is StatefulNode) return false
    return this.stopName == other.stopName
  }

  override fun hashCode(): Int {
    return stopName.hashCode()
  }

  /**
   * Returns the next node
   * representing a state of the passenger
   * if they follow the connection
   */
  fun evolve(connection: IConnection): StatefulNode {
    val currentLine = when (connection) {
      is TransferConnection -> connection.line
      else -> null
    }
    return StatefulNode (
      stopName = connection.end.name,
      time = time + connection.totalTimeCost(time),
      transfers = transfers + 1,
      ridesAndWalks = ridesAndWalks +
        if (lastLine == currentLine) 0 else 1,
      lastLine = currentLine,
    )
  }
}