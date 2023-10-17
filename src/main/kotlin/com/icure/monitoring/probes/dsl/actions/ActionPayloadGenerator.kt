package com.icure.monitoring.probes.dsl.actions

import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.actions.payload.JiraActionPayload
import com.icure.monitoring.actions.payload.LogActionPayload
import com.icure.monitoring.probes.dsl.comparators.ThresholdValue
import com.icure.monitoring.probes.dsl.descriptors.DescriptorElement


/**
 * Factory method for [ActionPayloadGenerator] that generates different instance for the different [ActionPayload]s
 */
interface ActionPayloadGeneratorFactory {
    /**
     * @return a [ActionPayloadGenerator] of [JiraActionPayload] that will generate the one specified in the block parameter.
     */
    fun jira(block: (value: Double, threshold: ThresholdValue, descriptors: Set<DescriptorElement>) -> JiraActionPayload): ActionPayloadGenerator<JiraActionPayload>

    /**
     * @return a [ActionPayloadGenerator] of [LogActionPayload] that will generate the one specified in the block parameter.
     */
    fun log(block: (value: Double, threshold: ThresholdValue, descriptors: Set<DescriptorElement>) -> LogActionPayload): ActionPayloadGenerator<LogActionPayload>
}

/**
 * A generic class that can generate a payload for an action, based on its generic type.
 */
class ActionPayloadGenerator<T : ActionPayload> private constructor(
    private val generator: (value: Double, threshold: ThresholdValue, descriptors: Set<DescriptorElement>) -> T
) {

    companion object : ActionPayloadGeneratorFactory {
        override fun jira(block: (value: Double, threshold: ThresholdValue, descriptors: Set<DescriptorElement>) -> JiraActionPayload): ActionPayloadGenerator<JiraActionPayload> =
            ActionPayloadGenerator(block)

        override fun log(block: (value: Double, threshold: ThresholdValue, descriptors: Set<DescriptorElement>) -> LogActionPayload): ActionPayloadGenerator<LogActionPayload> =
            ActionPayloadGenerator(block)
    }

    /**
     * Generates the payload for an action, which type depends on the generic type passed to the instance.
     *
     * @param value the [Double] value that triggered the action.
     * @param threshold the reference value.
     * @param descriptors a [Set] of [DescriptorElement] associated to the value.
     * @return an [ActionPayload].
     */
    fun generate(value: Double, threshold: ThresholdValue, descriptors: Set<DescriptorElement>) =
        generator(value, threshold, descriptors)

}