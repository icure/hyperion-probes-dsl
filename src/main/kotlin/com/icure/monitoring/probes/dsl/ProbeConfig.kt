package com.icure.monitoring.probes.dsl

import com.icure.monitoring.probes.Probe
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.IllegalStateException

/**
 * Configuration class to instantiate a probe.
 * @param description the description of the probe, used in the payload of the actions.
 * @param actions the actions that the probe will dispatch.
 * @param filter the filter applied to the incoming data.
 * @param probeId the id of the probe.
 */
@Serializable
data class ProbeConfig(
    var description: String = "",
    val actions: MutableList<ActionConfig<*>> = mutableListOf(),
    var filter: Filter = NoOpFilter,
    var probeId: String = UUID.randomUUID().toString(),
) {

    /**
     * The trigger defined through the DSL.
     */
    lateinit var definedTrigger: Trigger

    /**
     * The data source defined through the DSL.
     */
    lateinit var definedDataSource: DataSource

    /**
     * Checks if a probe can be instantiated through this configuration. To do so, both the trigger and the data source
     * must be defined.
     */
    fun isComplete() {
        when {
            !::definedTrigger.isInitialized -> throw IllegalStateException("No Trigger defined")
            !::definedDataSource.isInitialized -> throw IllegalStateException("No DataSource defined")
        }
    }

    /**
     * Defines the data source for this Probe. Can be defined only once per configuration.
     */
    @DataSourceScope
    fun dataSource(block: (@DataSourceScope DataSource.Companion).() -> DataSource) {
        definedDataSource = block(DataSource.Companion)
    }

    /**
     * Defines the trigger for this Probe. can be defined only once per configuration.
     */
    fun trigger(block: Trigger.Companion.() -> Trigger) {
        definedTrigger = block(Trigger.Companion)
    }

    /**
     * Defines a filter used to limit the data processed by the trigger. Can be defined only once per configuration.
     */
    @FilterScope
    fun filter(block: () -> Filter) {
        filter = block()
    }

    /**
     * Defines an action to be dispatched when the probe is triggered.
     */
    @ActionScope
    fun action(block: (@ActionScope ActionConfig.Companion).() -> ActionConfig<*>) {
        actions.add(block(ActionConfig.Companion))
    }

}

/**
 * Starting point for the probe DSL. It instantiates a [Probe] class according to the configuration specified.
 */
fun probe(init: ProbeConfig.() -> Unit): Probe = ProbeConfig().apply(init).also{
    it.isComplete()
}.let { it.definedDataSource.createProbe(it) }

/**
 * Generate a JSON configuration for a probe.
 */
fun probeConfig(init: ProbeConfig.() -> Unit): String = ProbeConfig().apply(init).let{
    it.isComplete()
    Json.encodeToString(it)
}
