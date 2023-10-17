package com.icure.monitoring.probes.dsl

import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.probes.Probe
import com.icure.monitoring.probes.dsl.actions.ActionPayloadGenerator
import com.icure.monitoring.probes.dsl.comparators.Comparator
import com.icure.monitoring.probes.dsl.comparators.ThresholdValue
import com.icure.monitoring.probes.dsl.descriptors.Descriptor
import com.icure.monitoring.probes.dsl.threshold.FixedValueThreshold
import com.icure.monitoring.probes.dsl.threshold.RegistryThreshold
import com.icure.monitoring.probes.dsl.threshold.Threshold
import io.micrometer.core.instrument.Meter
import java.util.UUID

/**
 * Configuration class to instantiate a probe.
 */
class ProbeConfig : DataAggregationChain() {

    /**
     * An Id that uniquely identifies the probe.
     */
    var probeId: String = UUID.randomUUID().toString()
    lateinit var descriptorsGenerator: (Meter) -> Collection<Descriptor>
    lateinit var comparator: Comparator
    lateinit var threshold: Threshold
    val definedActions = mutableListOf<ActionPayloadGenerator<*>>()

    /**
     * Defines a collection of [Descriptor]s that will be used to group the results.
     */
    fun groupBy(block: (Meter) -> Collection<Descriptor>) {
        descriptorsGenerator = block
    }

    /**
     * Defines a function that will compare the aggregated value to the threshold.
     */
    fun compareUsing(block: Comparator) {
        comparator = block
    }

    /**
     * Defines a complex threshold using a parallel [DataAggregationChain].
     */
    fun threshold(block: DataAggregationChain.() -> Unit) = DataAggregationChain().apply(block).also {
        threshold = RegistryThreshold(it)
    }

    /**
     * Defines a fixed size threshold.
     */
    fun fixedThreshold(block: () -> ThresholdValue) {
        threshold = FixedValueThreshold(block())
    }

    fun <T: ActionPayload> action(block: ActionPayloadGenerator.Companion.() -> ActionPayloadGenerator<T>) {
        definedActions.add(block(ActionPayloadGenerator.Companion))
    }

}

/**
 * Starting point for the probe DSL. It instantiates a [Probe] class according to the configuration specified.
 */
fun probe(init: ProbeConfig.() -> Unit): Probe = ProbeConfig().apply(init).let { it.definedDataSource.createProbe(it) }
