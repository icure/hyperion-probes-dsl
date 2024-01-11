package com.icure.monitoring.probes.dsl.filters

import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * A [Filter] that matches the [Meter]s that have the name specified as parameter.
 * On ElasticSearch, uses the match operation. That is why differs from [NameRegexFilter].
 *
 * @param query the name of the meter to match.
 */
@Serializable
data class MatchNameFilter(
    val query: String
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean = meter.id.name == query
    override fun toString(): String = "name matches $query"
    override fun toElasticQuery(): String = "\"match\":{\"s_qs_item-name\":\"$query\"}"
}

/**
 * Generates a [MatchNameFilter] for the name passed as parameter.
 *
 * @param name the metric name to match.
 * @return a [MatchNameFilter]
 */
fun metricNameIs(name: String) = MatchNameFilter(name)
