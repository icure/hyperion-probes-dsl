package com.icure.monitoring.probes.dsl.actions

import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.actions.payload.JiraActionPayload
import com.icure.monitoring.actions.payload.LogActionPayload
import com.icure.monitoring.probes.dsl.descriptors.DescriptorElement

/**
 * Represents a value registered by a probe that triggered the action execution.
 *
 * @param value the aggregated value.
 * @param descriptors the descriptors associated to the value
 */
data class TriggerValue(
    val value: Double,
    val descriptors: Set<DescriptorElement>
)

/**
 * Factory method for [ActionPayloadGenerator] that generates different instance for the different [ActionPayload]s
 */
interface ActionPayloadGeneratorFactory {
    /**
     * @return a [ActionPayloadGenerator] of [JiraActionPayload] that will generate the one specified in the block parameter.
     */
    fun jira(block: (TriggerValue) -> JiraActionPayload): ActionPayloadGenerator<JiraActionPayload>

    /**
     * @return a [ActionPayloadGenerator] of [LogActionPayload] that will generate the one specified in the block parameter.
     */
    fun log(block: (TriggerValue) -> LogActionPayload): ActionPayloadGenerator<LogActionPayload>
}

/**
 * A generic class that can generate a payload for an action, based on its generic type.
 */
class ActionPayloadGenerator<T : ActionPayload> private constructor(
    private val generator: (TriggerValue) -> T
) {

    companion object : ActionPayloadGeneratorFactory {
        override fun jira(block: (TriggerValue) -> JiraActionPayload): ActionPayloadGenerator<JiraActionPayload> =
            ActionPayloadGenerator(block)

        override fun log(block: (TriggerValue) -> LogActionPayload): ActionPayloadGenerator<LogActionPayload> =
            ActionPayloadGenerator(block)
    }

    /**
     * Generates the payload for an action, which type depends on the generic type passed to the instance.
     *
     * @param triggerValue the [TriggerValue] that triggered the execution of the action.
     * @return an [ActionPayload].
     */
    fun generate(triggerValue: TriggerValue) = generator(triggerValue)

}