package com.icure.monitoring.test.generators

import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.probes.dsl.descriptors.DescriptorElement
import com.icure.monitoring.probes.dsl.descriptors.NULL_GROUP
import com.icure.monitoring.probes.dsl.descriptors.NULL_VALUE
import io.micrometer.core.instrument.Meter

interface MeterGenerator<T: Meter> {
    fun generate(numSamples: Int): Sequence<T>
}

data class VariableTag(
    val tag: MetricsTags,
    val valuesGenerator: () -> String
)

val constantDescriptorElement = DescriptorElement(NULL_GROUP, NULL_VALUE)