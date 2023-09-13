package com.icure.monitoring.probes.dsl

import com.icure.monitoring.meters.BucketMeter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

data class MetricValue(
    val timestamp: Long?,
    val value: Double
)

/**
 * Base interface for all the metrics that can be used to activate a trigger on a probe.
 */
@Serializable
sealed interface Metric {
    /**
     * A descriptor used for visualization purposes.
     */
    val label: String

    /**
     * A descriptor of the extracted value used in ElasticSearch aggregations.
     */
    val field: String

    /**
     * Extracts the metrics from a [Meter].
     * @return a [MetricValue] that contains the value and the timestamp. The timestamp is the current timestamp unless
     * the meter specifies a timestamp attached to the value.
     */
    fun value(meter: Meter): MetricValue?
}

/**
 * Interface of the factory method used to instantiate the concrete classes for each metric, depending on the desired
 * aggregation.
 */
interface MetricFactory {
    fun forMaxAggregation(): Metric
    fun forAverageAggregation(): Metric
    fun forCountAggregation(): Metric
}

/**
 * Extracts the aggregated values from a distribution summary
 */
@Serializable
sealed class DistributionSummaryValue : Metric {
    @Transient
    override val label: String = "distribution summary"

    companion object: MetricFactory {
       override fun forMaxAggregation() = MaxOfDistributionSummary()
       override fun forAverageAggregation() = AverageOfDistributionSummary()
       override fun forCountAggregation() = CountOfDistributionSummary()
    }
}

/**
 * Maximum value of a distribution summary
 */
@Serializable
class MaxOfDistributionSummary : DistributionSummaryValue() {
    @Transient
    override val field = "max"
    override fun value(meter: Meter): MetricValue? = if(meter is DistributionSummary) {
        MetricValue(
            if(meter is BucketMeter<*>) meter.getTimestamps().max() else null,
            meter.takeSnapshot().max()
        )
    } else null
}

/**
 * Average value of a distribution summary
 */
@Serializable
class AverageOfDistributionSummary : DistributionSummaryValue() {
    @Transient
    override val field = "mean"
    override fun value(meter: Meter): MetricValue? = if(meter is DistributionSummary) {
        MetricValue(
            if(meter is BucketMeter<*>) meter.getTimestamps().maxOrNull() else null,
            meter.takeSnapshot().mean()
        )
    } else null
}

/**
 * Number of times the distribution summary is registered.
 */
@Serializable
class CountOfDistributionSummary : DistributionSummaryValue() {
    @Transient
    override val field = "count"
    override fun value(meter: Meter): MetricValue? = if(meter is DistributionSummary) {
        MetricValue(
            if(meter is BucketMeter<*>) meter.getTimestamps().maxOrNull() else null,
            meter.takeSnapshot().count().toDouble()
        )
    } else null
}

/**
 * Aggregates the value of a generic Gauge.
 */
@Serializable
class GaugeValue : Metric {
    @Transient
    override val label = "value"
    @Transient
    override val field: String = "value"

    companion object: MetricFactory {
        override fun forMaxAggregation() = GaugeValue()
        override fun forAverageAggregation() = GaugeValue()
        override fun forCountAggregation() = GaugeValue()
    }
    override fun value(meter: Meter): MetricValue? = if(meter is Gauge) {
        val gaugeValue = meter.value()
        MetricValue(null, gaugeValue).takeIf { gaugeValue.isFinite() }
    } else null

}