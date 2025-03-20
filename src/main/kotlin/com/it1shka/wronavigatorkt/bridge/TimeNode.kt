package com.it1shka.wronavigatorkt.bridge

/**
 * Additionally stores passenger time
 */
class TimeNode (val stopName: String, val time: Int) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TimeNode) return false
    return this.stopName == other.stopName
  }

  override fun hashCode(): Int {
    return stopName.hashCode()
  }
}