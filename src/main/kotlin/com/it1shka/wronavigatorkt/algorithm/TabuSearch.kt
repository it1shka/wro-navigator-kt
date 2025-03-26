package com.it1shka.wronavigatorkt.algorithm

import com.it1shka.wronavigatorkt.utils.variance

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
  private val stoppingVariance: Int? = null
) {
  private var step = 0
  private val tabu = mutableMapOf<S, Int>(initial to -1)
  private val memory = mutableListOf<Double>(cost(initial))
  private var localOptimum = initial
  private var globalBestCost = cost(initial)
  private var globalOptimum = initial

  fun run(): S {
    while (step < maxIterations) {
      localOptimum = findLocalOptimum() ?: return globalOptimum
      addToTabu(localOptimum)
      addToMemory(localOptimum)
      if (cost(localOptimum) > globalBestCost) {
        globalOptimum = localOptimum
        globalBestCost = cost(localOptimum)
      } else if (stoppingVariance != null) {
        val currentVariance = memory.variance()
        if (currentVariance < stoppingVariance) {
          return globalOptimum
        }
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