package com.icure.monitoring.probes.dsl.filters

import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * A [Filter] that negates the [Filter] passed as parameter: it matches all the [Meter]s that do NOT match the inner
 * filter.
 * On ElasticSearch, resolves in a boolean query where the inner filter is in the must_not statement.
 *
 * @param filter the [Filter] to negate.
 */
@Serializable
data class NotFilter(
    val filter: Filter
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean = !filter.matches(meter)
    override fun toString(): String = "NOT (${filter})"
    override fun toElasticQuery(): String = "\"bool\":{\"must_not\":[{${filter.toElasticQuery()}}]}"
}

/**
 * Negates the [Filter] passed as parameter.
 *
 * @param filter the [Filter] to negate.
 * @return a [NotFilter]
 */
fun not(filter: Filter): Filter = NotFilter(filter)
