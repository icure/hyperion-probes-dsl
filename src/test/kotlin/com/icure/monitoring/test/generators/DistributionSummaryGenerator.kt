package com.icure.monitoring.test.generators

import com.icure.monitoring.probes.dsl.descriptors.Descriptor
import com.icure.monitoring.probes.dsl.descriptors.DescriptorElement
import com.icure.monitoring.test.fake.PreSetDistributionSummary
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Tag

class DistributionSummaryGenerator(
    private val name: String,
    private val fixedTags: List<Tag>,
    private val variableTags: List<VariableTag>,
    private val countGenerator: () -> Long,
    private val sumGenerator: () -> Double,
    private val maxGenerator: () -> Double,
    private val descriptor: Descriptor? = null
) : MeterGenerator<DistributionSummary> {

    val countValues = mutableMapOf<Set<DescriptorElement>, List<Long>>()
    val sumValues = mutableMapOf<Set<DescriptorElement>, List<Double>>()
    val maxValues = mutableMapOf<Set<DescriptorElement>, List<Double>>()

    override fun generate(numSamples: Int): Sequence<DistributionSummary> =
        (0 until numSamples).asSequence().map { _ ->
            PreSetDistributionSummary(
                name,
                fixedTags + variableTags.map { Tag.of(it.tag.tagName, it.valuesGenerator()) },
                countGenerator(),
                sumGenerator(),
                maxGenerator()
            )
        }.onEach {
            countValues[setOf(constantDescriptorElement)] = countValues.getOrDefault(setOf(constantDescriptorElement), emptyList()) + it.count()
            sumValues[setOf(constantDescriptorElement)] = sumValues.getOrDefault(setOf(constantDescriptorElement), emptyList()) + it.totalAmount()
            maxValues[setOf(constantDescriptorElement)] = maxValues.getOrDefault(setOf(constantDescriptorElement), emptyList()) + it.max()

            descriptor?.also { desc ->
                countValues[setOf(desc(it))] = countValues.getOrDefault(setOf(desc(it)), emptyList()) + it.count()
                sumValues[setOf(desc(it))] = sumValues.getOrDefault(setOf(desc(it)), emptyList()) + it.totalAmount()
                maxValues[setOf(desc(it))] = maxValues.getOrDefault(setOf(desc(it)), emptyList()) + it.max()
            }
        }
}

