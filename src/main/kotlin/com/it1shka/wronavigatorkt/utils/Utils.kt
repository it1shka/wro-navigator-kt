package com.it1shka.wronavigatorkt.utils

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

fun String.toTimeValue(applyModulus: Boolean = true): Int? {
  val parts = this
    .split(':')
    .mapNotNull { it.toIntOrNull() }
  if (parts.size != 3) return null
  val (hours, minutes, seconds) = parts
  var totalValue = hours * 3600 + minutes * 60 + seconds
  if (applyModulus) {
    val totalDayTime = 24 * 60 * 60
    totalValue %= totalDayTime
  }
  return totalValue
}

fun Int.toTimeString(): String {
  val seconds = (this % 60).toString().padStart(2, '0')
  val minutes = ((this / 60) % 60).toString().padStart(2, '0')
  val hours = (this / 3600).toString().padStart(2, '0')
  return "${hours}:${minutes}:${seconds}"
}

fun Int.toTimeDescription(): String {
  if (this <= 0) return "0s"
  val seconds = this % 60
  val minutes = (this / 60) % 60
  val hours = this / 3600
  return listOf(hours to "h", minutes to "m", seconds to "s")
    .filter { (value, _) -> value > 0 }
    .joinToString(" ") { (value, postfix) -> "$value$postfix" }
}

fun Double.toDistanceDescription(): String {
  if (this < 1.0) {
    val meters = (this * 1000).roundToInt()
    return "${meters}m"
  }
  val kilometers = round(this * 10) / 10.0
  return "${kilometers}km"
}

fun <A, B> Pair<A, A>.map(f: (A) -> B): Pair<B, B> {
  val (first, second) = this
  return f(first) to f(second)
}

fun degreeToRad(degrees: Double): Double {
  return degrees * Math.PI / 180.0
}

/**
 * Returns a distance on a globe (Earth sphere).
 * The output value is measured in kilometers
 */
fun haversine(start: Pair<Double, Double>, end: Pair<Double, Double>): Double {
  val (f1, l1) = start.map { degreeToRad(it) }
  val (f2, l2) = end.map { degreeToRad(it) }
  val earthRadius = 6371
  val sqrtArg = sin((f2 - f1) / 2).pow(2.0) + sin((l2 - l1) / 2).pow(2.0) * cos(f1) * cos(f2)
  return 2 * earthRadius * sqrt(sqrtArg)
}

/**
 * Computes distance between two strings
 */
fun levenshtein(s: String, t: String): Int {
  if (s.isEmpty()) return t.length
  if (t.isEmpty()) return s.length
  val arr = Array(t.length + 1) {
    IntArray(s.length + 1)
  }
  for (i in 0 .. t.length) {
    arr[i][0] = i
    for (j in 1 .. s.length) {
      arr[i][j] = if (i == 0) j else minOf(
        arr[i - 1][j] + 1,
        arr[i][j - 1] + 1,
        arr[i - 1][j - 1] + if (s[j - 1] == t[i - 1]) 0 else 1,
      )
    }
  }
  return arr[t.length][s.length]
}

fun timeDistance(from: Int, to: Int): Int {
  if (from <= to) {
    return to - from
  }
  val totalDayTime = 24 * 60 * 60
  return totalDayTime - from + to
}

fun <T, K> List<T>.groupConsecutiveBy(selector: (T) -> K): List<List<T>> {
  return fold(mutableListOf<MutableList<T>>()) { acc, item ->
    if (acc.isEmpty() || selector(acc.last().last()) != selector(item)) {
      acc.add(mutableListOf(item))
    } else {
      acc.last().add(item)
    }
    acc
  }
}
