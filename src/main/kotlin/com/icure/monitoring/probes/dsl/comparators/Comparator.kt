package com.icure.monitoring.probes.dsl.comparators

typealias ThresholdValue = Double

/**
 * A [Comparator] is a function that defines a condition on two [Double] values and return the result of that condition.
 * This will be used to check whether trigger the actions associated to a probe when receiving a value, so the condition
 * should be true if the actions should be triggered and false otherwise.
 * The first value is the value produced by the aggregator, the second is a threshold to compare with.
 */
typealias Comparator = (Double, ThresholdValue) -> Boolean