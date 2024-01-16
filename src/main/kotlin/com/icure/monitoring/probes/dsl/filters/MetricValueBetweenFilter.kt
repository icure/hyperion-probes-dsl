package com.icure.monitoring.probes.dsl.filters

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * A [Filter] that matches the [Meter]s that are gauges and have their value within the range specified as parameter.
 * Bounds are included in the comparison.
 * On ElasticSearch, uses the range operation.
 *
 * @param from the lower bound of the range to match.
 * @param to the upper bound of the range to match.
 */
@Serializable
data class MetricValueBetweenFilter(
    val from: Double,
    val to: Double,
    val valueField: String,
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean = meter is Gauge && meter.value() >= from && meter.value() <= to
    override fun toString(): String = "$from <= value <= $to"
    override fun toElasticQuery(): String = "\"range\":{\"$valueField\":{\"gte\":$from,\"lte\":$to}}"
}

/**
 * Generates a [MetricValueBetweenFilter] for the bounds passed as parameters.
 *
 * @param from the lower bound of the range to match.
 * @param to the upper bound of the range to match.
 * @return a [MetricValueBetweenFilter]
 */
fun metricValueBetween(from: Double, to: Double, valueField: String = "value") = MetricValueBetweenFilter(from, to, valueField)
