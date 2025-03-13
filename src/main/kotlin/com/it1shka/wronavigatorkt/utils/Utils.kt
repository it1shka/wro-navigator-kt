package com.it1shka.wronavigatorkt.utils

fun String.toTimeValue(): Int? {
  val parts = this
    .split(':')
    .map { it.toIntOrNull() }
    .filterNotNull()
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