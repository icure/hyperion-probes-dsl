package com.icure.monitoring.probes

import com.icure.monitoring.actions.Action
import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.probes.dsl.ProbeConfig
import com.icure.monitoring.probes.dsl.collectors.Collector
import com.icure.monitoring.probes.dsl.descriptors.DescriptorElement
import com.icure.monitoring.probes.dsl.threshold.RegistryThreshold
import io.micrometer.core.instrument.Meter
import java.util.concurrent.ConcurrentHashMap

/**
 * Concrete probe that can be attached to a micrometer registry.
 *
 * @param registryId the id of the registry that this probe should lister,
 * @param config a [ProbeConfig].
 */
class RegistryProbe(
	val registryId: String,
	config: ProbeConfig
) : Probe(config) {

	private val collectors = ConcurrentHashMap<Set<DescriptorElement>, Collector>()
	private val newCollector = config.collectorProducer

	/**
	 * Receives a [Meter] from a registry and store its value if:
	 * - the meter has a non-null value.
	 * - the registry that is submitting them meter has the same ID as the one defined when instantiating the probe.
	 * - the meter matches the [com.icure.monitoring.probes.dsl.filters.Filter] defined in the probe.
	 *
	 * @param meter the [Meter] to receive.
	 * @param submittingRegistryId the id of the submitting registry.
	 */
	suspend fun receiveMeter(meter: Meter, submittingRegistryId: String) {
		canTriggerActions = true
		val extractedValue = extractor.valueOf(meter)
		if(extractedValue != null && registryId == submittingRegistryId && filter.matches(meter)) {
			val descriptors = descriptorsGenerator(meter)
			collectors.getOrPut(descriptors.toSet()) { newCollector() }.addValue(extractedValue)
		}
		if(threshold is RegistryThreshold) {
			threshold.receiveMeter(meter, submittingRegistryId)
		}
	}

	/**
	 * Aggregates the values collected and compares them against the defined threshold using the
	 * [com.icure.monitoring.probes.dsl.comparators.Comparator] defined in the config.
	 * If the comparator returns true, then the appropriate actions are dispatched.
	 *
	 * @param availableActions the actions registered in the system.
	 */
	suspend fun checkAndDispatch(availableActions: List<Action<ActionPayload>>) {
		val thresholdValue = threshold.getValue()
		collectors.forEach { (descriptors, collector) ->
			dispatchActionsOnTriggerActivation(
				aggregator.aggregate(collector),
				thresholdValue,
				descriptors,
				availableActions
			)
		}
	}

}
