package com.icure.monitoring.probes.dsl.descriptors

import io.micrometer.core.instrument.Meter

/**
 * The [Descriptor] type is a type alias for a function that takes as input a [Meter] and returns a [String].
 * This value will be then used to aggregate the results of the different meters separately.
 */
typealias Descriptor = (Meter) -> String

/**
 * Utility function to easily define a custom [Descriptor].
 */
fun descriptor(block: Descriptor) = block