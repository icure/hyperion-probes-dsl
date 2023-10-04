package com.icure.monitoring.probes.dsl.comparators

typealias ThresholdValue = Double

/**
 * A [Comparator] is a function that defines a condition on two [Double] values and return the result of that condition.
 * The first value is the value produced by the aggregator, the second is a threshold to compare with.
 */
typealias Comparator = (Double, ThresholdValue) -> Boolean

/**
 * Utility function to easily define a [Comparator].
 */
fun comparator(block: Comparator) = block