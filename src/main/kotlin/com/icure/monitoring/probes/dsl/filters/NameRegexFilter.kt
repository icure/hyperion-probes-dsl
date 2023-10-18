package com.icure.monitoring.probes.dsl.filters

import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * A [Filter] that matches a [Meter] if its name matches the regex pattern passed as parameter.
 * On ElasticSearch, uses the regexp operation.
 *
 * @param pattern a regex pattern used in the match.
 */
@Serializable
data class NameRegexFilter(
    val pattern: String
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean = Regex(pattern).containsMatchIn(meter.id.name)
    override fun toString(): String = "name matches $pattern"
    override fun toElasticQuery(): String = "\"regexp\":{\"name\":{\"value\":\"$pattern\"}}"
}

/**
 * Creates a [NameRegexFilter] for the pattern passed as parameter.
 *
 * @param pattern a regex pattern to match the name of the [Meter].
 * @return a [NameRegexFilter]
 */
fun metricNameMatches(pattern: String) = NameRegexFilter(pattern)