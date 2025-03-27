package com.it1shka.wronavigatorkt.algorithm

/**
 * Class representing Tabu Search.
 * Contains configuration for the search.
 * Does not optimize function calls
 */
class TabuSearch <S> (
  initial: S,
  private val neighborhood: (S) -> Sequence<S>,
  private val cost: (S) -> Double,
  private val maxIterations: Int,
  private val memorySize: Int = 5,
  private val aspiration: ((List<Double>, Double) -> Boolean)? = null,
  private val tabuLimit: Int? = null,
  private val repetitionsLimit: Int? = null,
  private val onChange: ((S, S, Double, Double) -> Unit)? = null
) {
  private var step = 0
  private val tabu = mutableMapOf<S, Int>(initial to -1)
  private val memory = mutableListOf<Double>(cost(initial))
  private var localOptimum = initial
  private var globalBestCost = cost(initial)
  private var globalOptimum = initial
  private var repetitions = 0

  fun run(): S {
    while (step < maxIterations) {
      val nextLocalOptimum = findLocalOptimum() ?: return globalOptimum
      repetitions = if (nextLocalOptimum == localOptimum)
        repetitions + 1
        else 0
      if (repetitionsLimit != null && repetitions > repetitionsLimit) {
        return globalOptimum
      }
      localOptimum = nextLocalOptimum
      addToTabu(localOptimum)
      addToMemory(localOptimum)
      if (cost(localOptimum) < globalBestCost) {
        onChange?.invoke(globalOptimum, localOptimum, globalBestCost, cost(localOptimum))
        globalOptimum = localOptimum
        globalBestCost = cost(localOptimum)
      }
      step++
    }
    return globalOptimum
  }

  private fun findLocalOptimum(): S? {
    val neighbors = neighborhood(localOptimum).asSequence()
    val feasibleNeighbors = neighbors.filter filter@ { neighbor ->
      when {
        neighbor !in tabu -> true
        aspiration == null -> false
        else -> aspiration(memory, cost(neighbor))
      }
    }
    return feasibleNeighbors.minByOrNull { neighbor ->
      cost(neighbor)
    }
  }

  private fun addToTabu(solution: S) {
    tabu[solution] = step
    if (tabuLimit == null) {
      return
    }
    val oldest = tabu.entries
      .asSequence()
      .minBy { it.value }
      .key
    tabu.remove(oldest)
  }

  private fun addToMemory(solution: S) {
    memory.add(cost(solution))
    if (memory.size <= memorySize) {
      return
    }
    memory.removeAt(0)
  }
}