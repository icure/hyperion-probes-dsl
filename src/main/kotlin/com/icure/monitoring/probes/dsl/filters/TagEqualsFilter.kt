package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.model.MetricsTags
import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * A [Filter] that matches all the [Meter]s that have at least one tag of the specified type which equals
 * the value passed as parameter.
 * On ElasticSearch, uses the term operation.
 *
 * @param tag the [MetricsTags] to look for in the [Meter].
 * @param value the value of the tag
 */
@Serializable
data class TagEqualsFilter(
    val tag: MetricsTags,
    val value: String
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean = meter.id.tags.firstOrNull { it.key == tag.tagName }?.let {
        value == it.value
    } != null
    override fun toString(): String = "${tag.tagName} is $value"
    override fun toElasticQuery(): String = "\"term\":{\"${tag.queryValue}\":\"$value\"}"
}

/**
 * Returns a [TagEqualsFilter] that uses as tag type the [MetricsTags] used as receiver parameter and as value the
 * one passed as parameter.
 *
 * @param value a value that will be used to check the value of the tag.
 * @return a [TagEqualsFilter]
 */
infix fun MetricsTags.isEqualTo(value: String) = TagEqualsFilter(this, value)
