package com.icure.monitoring.probes.dsl.filters

import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * A [Filter] that matches the [Meter]s that have the name specified as parameter.
 * On ElasticSearch, uses the term operation. That is why differs from [NameRegexFilter].
 *
 * @param query the name of the meter to match.
 */
@Serializable
data class NameEqualsFilter(
    val query: String
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean = meter.id.name == query
    override fun toString(): String = "name is $query"
    override fun toElasticQuery(): String = "\"term\":{\"name\":\"$query\"}"
}

/**
 * Generates a [NameEqualsFilter] for the name passed as parameter.
 *
 * @param name the metric name to match.
 * @return a [NameEqualsFilter]
 */
fun metricNameIs(name: String) = NameEqualsFilter(name)
