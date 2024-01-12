package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.model.MetricsTags
import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * A [Filter] that matches all the [Meter]s that have at least one tag of the specified type which value matches
 * the regex pattern passed as parameter.
 * On ElasticSearch, uses the regexp operation.
 *
 * @param tag the [MetricsTags] to look for in the [Meter].
 * @param pattern a regex string that will be used for the match
 */
@Serializable
data class TagRegexFilter(
    val tag: MetricsTags,
    val pattern: String
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean = meter.id.tags.firstOrNull { it.key == tag.tagName }?.let {
        Regex(pattern).find(it.value)
    } != null
    override fun toString(): String = "${tag.tagName} matches $pattern"
    override fun toElasticQuery(): String = "\"regexp\":{\"${tag.queryValue}\":{\"value\":\"$pattern\"}}"
}

/**
 * Returns a [TagRegexFilter] that uses as tag type the [MetricsTags] used as receiver parameter and as regex the
 * one passed as parameter.
 *
 * @param pattern a regex pattern that will be used to check the value of the tag.
 * @return a [TagRegexFilter]
 */
infix fun MetricsTags.matches(pattern: String) = TagRegexFilter(this, pattern)
