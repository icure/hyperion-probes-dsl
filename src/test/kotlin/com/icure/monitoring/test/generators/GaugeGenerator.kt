package com.icure.monitoring.test.generators

import com.icure.monitoring.probes.dsl.descriptors.Descriptor
import com.icure.monitoring.probes.dsl.descriptors.DescriptorElement
import com.icure.monitoring.test.generateGauge
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Tag

class GaugeGenerator(
    private val nameGenerator: () -> String,
    private val fixedTags: List<Tag>,
    private val variableTags: List<VariableTag>,
    private val valueGenerator: () -> Double,
    private val descriptor: Descriptor? = null
) : MeterGenerator<Gauge> {

    val values = mutableMapOf<Set<DescriptorElement>, List<Double>>()

    override fun generate(numSamples: Int): Sequence<Gauge> =
        (0 until numSamples).asSequence().map { _ ->
            generateGauge(
                nameGenerator(),
                fixedTags + variableTags.map { Tag.of(it.tag.tagName, it.valuesGenerator()) },
                valueGenerator()
            )
        }.onEach {
            values[setOf(constantDescriptorElement)] = values.getOrDefault(setOf(constantDescriptorElement), emptyList()) + it.value()

            descriptor?.also { desc ->
                values[setOf(desc(it))] = values.getOrDefault(setOf(desc(it)), emptyList()) + it.value()
            }
        }
}