package com.icure.monitoring.probes

import com.icure.monitoring.actions.Action
import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.probes.dsl.ProbeConfig
import com.icure.monitoring.probes.dsl.actions.ActionPayloadGenerator
import com.icure.monitoring.probes.dsl.comparators.ThresholdValue
import com.icure.monitoring.probes.dsl.descriptors.DescriptorElement
import io.micrometer.core.instrument.Meter
import com.icure.monitoring.probes.ElasticProbe.Companion.DescriptorsWithValue

/**
 * Base class for all the Probes.
 */
open class Probe(
	config: ProbeConfig
) {
	private val actionGenerators: List<ActionPayloadGenerator<*>> = config.definedActions
	val id = config.probeId
	val timestampField = config.timestampField
	val filter = config.definedFilter
	val threshold = config.threshold
	val trigger = config.comparator
	val aggregator = config.definedAggregator
	val extractor = config.definedExtractor

	protected val descriptorsGenerator: (Meter) -> List<DescriptorElement> = { meter ->
		config.descriptorsGenerator(meter).map { descriptor ->
			descriptor(meter)
		}
	}

	/**
	 * Checks if the value passed as parameter activates the trigger. In this case, dispatch the actions defined in the
	 * configuration.
	 * @param currentLevel the value that may activate the trigger.
	 * @param thresholdValue the reference value.
	 * @param descriptors a [Set] of [DescriptorElement]s associated to the current level.
	 * @param availableActions all the [Action] available on the system.
	 */
	suspend fun dispatchActionsOnTriggerActivation(
		currentLevel: Double?,
		thresholdValue: ThresholdValue?,
		descriptors: Set<DescriptorElement>,
		availableActions: List<Action<ActionPayload>>
	) {
		if (currentLevel != null && thresholdValue != null && trigger(currentLevel, thresholdValue)) {
			val payloads = actionGenerators.map { it.generate(currentLevel, thresholdValue, descriptors) }
			availableActions.forEach { it.acceptAndDispatch(payloads, descriptors) }
		}
	}
}

/**
 * Abstract class that should be implemented by all the concrete probes that fetch their data and dispatch their actions
 * asynchronously.
 */
abstract class SchedulableProbe(
	val cron: String,
	config: ProbeConfig
) : Probe(config) {

	/**
	 * Fetches the data from a remote source and aggregates them, providing a value that shall be then passed to the
	 * trigger.
	 */
	abstract suspend fun fetchData(elasticUrl: String, elasticUsername: String?, elasticPassword: String?, elasticHeaders: Map<String, String>?): Set<DescriptorsWithValue>?

}
