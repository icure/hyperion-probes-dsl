package com.icure.monitoring.probes.dsl

import com.icure.monitoring.meters.BucketMeter
import com.icure.monitoring.model.MetricsTags
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
     * An additional [Filter] that could be required to identify the metric. Must be specified at compile time.
     */
    val identifier: Filter

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
 * Measures the total time of a request.
 */
@Serializable
sealed class TotalTime : Metric {
    @Transient
    override val label: String = "total request time"
    @Transient
    override val identifier: Filter = (MetricsTags.METRIC matches "totalTime") and (MetricsTags.TYPE matches "distribution_summary")

    companion object: MetricFactory {
       override fun forMaxAggregation() = MaxTotalTime()
       override fun forAverageAggregation() = AverageTotalTime()
       override fun forCountAggregation() = CountTotalTime()
    }
}

/**
 * Maximum of request total time
 */
@Serializable
class MaxTotalTime : TotalTime() {
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
 * Average of request total time
 */
@Serializable
class AverageTotalTime : TotalTime() {
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
 * Number of times the Total Time is registered.
 */
@Serializable
class CountTotalTime : TotalTime() {
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
 * Counts the number of requests to the system.
 */
@Serializable
class RequestCount : Metric {
    @Transient
    override val label: String = "request count"
    @Transient
    override val identifier: Filter = (MetricsTags.METRIC matches "totalTime") and (MetricsTags.TYPE matches "distribution_summary")
    @Transient
    override val field = "count"

    companion object: MetricFactory {
        override fun forMaxAggregation() = RequestCount()
        override fun forAverageAggregation() = RequestCount()
        override fun forCountAggregation() = RequestCount()
    }
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
    override val identifier: Filter = MetricsTags.TYPE matches "gauge"
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