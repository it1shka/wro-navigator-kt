package com.it1shka.wronavigatorkt.utils

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

fun String.toTimeValue(): Int? {
  val parts = this
    .split(':')
    .mapNotNull { it.toIntOrNull() }
  if (parts.size != 3) return null
  val (hours, minutes, seconds) = parts
  return hours * 3600 + minutes * 60 + seconds
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
  return listOf(seconds to "s", minutes to "m", hours to "h")
    .filter { (value, _) -> value > 0 }
    .joinToString(" ") { (value, postfix) -> "$value$postfix" }
}

fun <A, B> Pair<A, A>.map(f: (A) -> B): Pair<B, B> {
  val (first, second) = this
  return f(first) to f(second)
}

fun degreeToRad(degrees: Double): Double {
  return degrees * Math.PI / 180
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