package com.icure.monitoring.probes.dsl.descriptors

/**
 * A [Descriptor] based on the name of the meter.
 */
val byName: Descriptor = { meter -> DescriptorElement("name", meter.id.name) }