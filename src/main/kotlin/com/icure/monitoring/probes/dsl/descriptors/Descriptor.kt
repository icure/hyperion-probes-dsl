package com.icure.monitoring.probes.dsl.descriptors

import io.micrometer.core.instrument.Meter

const val NULL_GROUP = "NULL_GROUP"
const val NULL_VALUE = "NULL_VALUE"

/**
 * A data class that represent one element of a complex descriptor
 *
 * @param k the key of the descriptor. It can be the tag key of a [Meter] or another property name.
 * @param v the value of the descriptor. It can be the tag value of a [Meter] or another property value
 */
data class DescriptorElement(
    val k: String,
    val v: String
)

/**
 * The [Descriptor] type is a type alias for a function that takes as input a [Meter] and returns a [DescriptorElement].
 * This value will be then used to aggregate the results of the different meters separately.
 */
typealias Descriptor = (Meter) -> DescriptorElement

/**
 * Utility function to easily define a custom [Descriptor].
 */
fun descriptor(block: Descriptor) = block