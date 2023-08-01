package com.icure.monitoring.probes.dsl

import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.actions.payload.JiraActionPayload
import com.icure.monitoring.actions.payload.LogActionPayload
import com.icure.monitoring.model.LogLevel
import com.icure.monitoring.probes.dsl.serialization.GenericActionConfigSerializer
import com.icure.monitoring.probes.dsl.serialization.JiraActionConfigSerializer
import com.icure.monitoring.probes.dsl.serialization.LogActionConfigSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Duration

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class ActionScope

/**
 * Base class for all the classes that can be set up through the DSL to configure the action a probe can dispatch.
 * It has a generic parameter that is an [ActionPayload], that is the data to pass to the action when dispatched.
 */
@Serializable(with = GenericActionConfigSerializer::class)
sealed class ActionConfig<T : ActionPayload> {

    abstract val actionClass: String

    companion object {
        /**
         * Configures a Jira Action to be dispatched.
         */
        @ActionScope
        fun jira(block: JiraActionConfig.() -> Unit) = JiraActionConfig().apply(block)

        /**
         * Configures a Log Action to be dispatched.
         */
        @ActionScope
        fun log(block: LogActionConfig.() -> Unit) = LogActionConfig().apply(block)
    }

    /**
     * Generates the payload that will be used by the action. It is based on the [Trigger] set up in the DSL and on
     * the actual value registered.
     * @param trigger the [Trigger] configured in the Probe DSL.
     * @param actualValue the value registered.
     */
    abstract fun generatePayload(trigger: Trigger, actualValue: Double, probeConfig: ProbeConfig): T

}

/**
 * Configuration class for a Jira Action.
 */
@Serializable(with = JiraActionConfigSerializer::class)
@ActionScope
class JiraActionConfig : ActionConfig<JiraActionPayload>() {

    @Transient
    override val actionClass = "com.icure.monitoring.actions.JiraAction"

    /**
     * The number of milliseconds after the last modification after which the jira ticket will be automatically closed.
     */
    var autoCloseAfter: Duration? = null

    override fun generatePayload(trigger: Trigger, actualValue: Double, probeConfig: ProbeConfig): JiraActionPayload = JiraActionPayload(
        probeId = probeConfig.probeId,
        title = buildString {
            append(trigger.label.replaceFirstChar { it.uppercaseChar() })
            append(" ${trigger.metric.label} was ${trigger.activationCondition.label} ${trigger.threshold}")
        },
        description = buildString {
            append("Registered value: $actualValue\n")
            append("Probe: ${probeConfig.description}\n")
            append("Filters: ${probeConfig.filter}")
        },
        autoCloseAfter = autoCloseAfter?.toMillis()
    )

}

/**
 * Configuration class for a Log Action.
 */
@Serializable(with = LogActionConfigSerializer::class)
@ActionScope
class LogActionConfig: ActionConfig<LogActionPayload>() {

    @Transient
    override val actionClass = "com.icure.monitoring.actions.LogAction"

    /**
     * The log level.
     */
    var level: LogLevel = LogLevel.INFO

    override fun generatePayload(trigger: Trigger, actualValue: Double, probeConfig: ProbeConfig): LogActionPayload = LogActionPayload(
        log = buildString {
            append(trigger.label.replaceFirstChar { it.uppercaseChar() })
            append(" ${trigger.metric.label} was ${trigger.activationCondition.label} ${trigger.threshold} ")
            append("(Registered: ${actualValue})")
        },
        level
    )
}