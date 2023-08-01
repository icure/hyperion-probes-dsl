package com.icure.monitoring.probes

import com.icure.monitoring.actions.Action
import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.probes.dsl.ActionConfig
import com.icure.monitoring.probes.dsl.ProbeConfig
import com.icure.monitoring.probes.dsl.probeConfig
import kotlinx.serialization.json.Json

/**
 * Base class for all the Probes.
 */
open class Probe(
   private val config: ProbeConfig
) {
    val id = config.probeId
    val description = config.description
    val actions: List<ActionConfig<*>> = config.actions
    val filter = config.filter and config.definedTrigger.metric.identifier
    val trigger = config.definedTrigger

    companion object {

        /**
         * Instantiates a new probe from a Json configuration obtained through the [probeConfig] method.
         */
        fun fromJsonConfiguration(config: String): Probe = Json.decodeFromString<ProbeConfig>(config).let {
            it.isComplete()
            it.definedDataSource.createProbe(it)
        }

    }

    /**
     * Checks if the value passed as parameter activates the trigger. In this case, dispatch the actions defined in the
     * configuration.
     * @param currentLevel the value that may activate the trigger.
     * @param availableActions all the [Action] available on the system.
     */
    fun dispatchActionsOnTriggerActivation(currentLevel: Double, availableActions: List<Action<ActionPayload>>) {
        if (trigger.checkThreshold(currentLevel)) {
            actions.forEach { actionConfig ->
                availableActions
                    .firstOrNull { it::class.qualifiedName == actionConfig.actionClass }?.execute(
                        actionConfig.generatePayload(trigger, currentLevel, config)
                    )
            }
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
    abstract suspend fun fetchData(): Double?

}