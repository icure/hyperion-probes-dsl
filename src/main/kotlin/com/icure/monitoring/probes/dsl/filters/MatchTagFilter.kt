package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.model.MetricsTags
import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * A [Filter] that matches all the [Meter]s that have at least one tag of the specified type which value matches
 * the regex pattern passed as parameter.
 * On ElasticSearch, uses the match operation.
 *
 * @param tag the [MetricsTags] to look for in the [Meter].
 * @param matchValue a regex string that will be used for the match
 */
@Serializable
data class MatchTagFilter(
    val tag: MetricsTags,
    val matchValue: String
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean = meter.id.tags.firstOrNull { it.key == tag.tagName }?.let {
        Regex(matchValue).find(it.value)
    } != null
    override fun toString(): String = "${tag.tagName} matches $matchValue"
    override fun toElasticQuery(): String = "\"match\":{\"${tag.queryValue}\":\"$matchValue\"}"
}

/**
 * Returns a [MatchTagFilter] that uses as tag type the [MetricsTags] used as receiver parameter and as regex the
 * one passed as parameter.
 *
 * @param value a regex pattern that will be used to check the value of the tag.
 * @return a [MatchTagFilter]
 */
infix fun MetricsTags.matches(value: String) = MatchTagFilter(this, value)