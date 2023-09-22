package com.icure.monitoring.probes.dsl

import com.icure.monitoring.meters.HistogramBucket
import com.icure.monitoring.probes.dsl.serialization.TriggerSerializer
import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable
import java.time.Duration
import kotlin.properties.Delegates

/**
 * Defines a condition over a metric. The probe will test the condition to dispatch the actions.
 */
@Serializable(with = TriggerSerializer::class)
abstract class Trigger {
    /**
     * A label for the trigger, used for visualization purposes.
     */
    abstract val label : String

    /**
     * A time window to aggregate the data over.
     */
    lateinit var timeFrame: Duration

    /**
     * A condition to test to check if the trigger activates.
     */
    lateinit var activationCondition: ActivationCondition

    /**
     * The threshold of the condition.
     */
    var threshold by Delegates.notNull<Double>()

    /**
     * The metric to test
     */
    lateinit var metric: Metric

    /**
     * A metric factory that will generate the appropriate metric for the desired aggregation.
     */
    protected lateinit var metricFactory: MetricFactory

    companion object {

        enum class ActivationCondition(val label: String, val condition: (Double, Double) -> Boolean) {
            GREATER_THAN("greater than", { value, threshold -> value > threshold}),
            LESS_THAN("less than", { value, threshold -> value < threshold})
        }

        /**
         * Instantiates a trigger that will use the maximum of the desired metric.
         */
        fun max(block: Trigger.() -> Unit): Trigger = MaxTrigger().apply{
            this.block()
            metric = metricFactory.forMaxAggregation()
        }

        /**
         * Instantiates a trigger that will use the average of the desired metric.
         */
        fun average(block: Trigger.() -> Unit): Trigger = AverageTrigger().apply {
            this.block()
            metric = metricFactory.forAverageAggregation()
        }

        /**
         * Instantiates a trigger that will use the count of the desired metric.
         */
        fun count(block: Trigger.() -> Unit): Trigger = CountTrigger().apply{
            this.block()
            metric = metricFactory.forCountAggregation()
        }
    }

    /**
     * Receives a value and returns true if the value activates the trigger, false otherwise.
     * @param value the value to test.
     */
    abstract fun checkThreshold(value: Double): Boolean

    /**
     * Given a collection of [HistogramBucket] calculates the metric aggregation as defined in the DSL.
     */
    abstract fun calculateCurrentLevel(input: Collection<Meter>): Double?

    /**
     * Converts the current trigger condition to an ElasticSearch aggregation.
     */
    abstract fun toElasticAggregation(): String

    /**
     * Extracts the value from a [Meter] using the strategy defined by the concrete implementation of the [Metric]
     * defined in this trigger.
     * Note: this should return the value for a single meter. Aggregating the different values is then a responsibility
     * of the specific implementation of the trigger.
     *
     * @param meter the [Meter] where to extract the value
     * @return an instance of [MetricValue] or null if the operation failed
     */
    fun extractValueFromMeter(meter: Meter): MetricValue? = metric.value(meter)

    /**
     * Specifies a [Metric] and a time window for the metric values' aggregation.
     */
    infix fun MetricFactory.over(duration: Duration): Trigger {
        metricFactory = this
        timeFrame = duration
        return this@Trigger
    }

    /**
     * Specifies the trigger condition as greater than the threshold.
     */
    infix fun greaterThan(threshold: Double): Trigger {
        activationCondition = ActivationCondition.GREATER_THAN
        this.threshold = threshold
        return this
    }

    /**
     * Specifies the trigger condition as less than the threshold.
     */
    infix fun lessThan(threshold: Double): Trigger {
        activationCondition = ActivationCondition.LESS_THAN
        this.threshold = threshold
        return this
    }
}

/**
 * This concrete trigger will compare the maximum of the [Metric] value over the specified time window to the specified
 * threshold level.
 */
@Serializable
class MaxTrigger: Trigger() {
    override val label = "max of"
    override fun checkThreshold(value: Double): Boolean = activationCondition.condition(value, threshold)
    override fun calculateCurrentLevel(input: Collection<Meter>): Double? = input.maxOfOrNull { it.max }
    override fun toElasticAggregation(): String = "{\"max\":{\"field\":\"${metric.field}\"}}"
}

/**
 * This concrete trigger will compare the average of the [Metric] value over the specified time window to the specified
 * threshold level.
 */
@Serializable
class AverageTrigger: Trigger() {
    override val label = "average of"
    override fun checkThreshold(value: Double): Boolean = activationCondition.condition(value, threshold)
    override fun calculateCurrentLevel(input: Collection<HistogramBucket>): Double? = input.takeIf { it.isNotEmpty() }?.map { it.sum / it.histogram.totalCount }?.average()
    override fun toElasticAggregation(): String = "{\"avg\":{\"field\":\"${metric.field}\"}}"
}

/**
 * This concrete trigger will compare the count of the [Metric] value over the specified time window to the specified
 * threshold level.
 */
@Serializable
class CountTrigger: Trigger() {
    override val label = "count of"
    override fun checkThreshold(value: Double): Boolean = activationCondition.condition(value, threshold)
    override fun calculateCurrentLevel(input: Collection<HistogramBucket>): Double? = input.takeIf { it.isNotEmpty() }?.sumOf { it.sum }
    override fun toElasticAggregation(): String = "{\"sum\":{\"field\":\"${metric.field}\"}}"
}


