package com.icure.monitoring.probes.dsl.filters

import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * Aggregates several [Filter]s through an AND operation.
 * On ElasticSearch, resolves in a boolean query where all the inner filters are in the must statement.
 *
 * @param filters a [List] of [Filter] to combine.
 */
@Serializable
data class AndFilter(
    val filters: List<Filter>
) : Filter {
    override infix fun and(other: Filter) =
        when(other) {
            is AndFilter -> copy(filters = filters + other.filters)
            is OrFilter -> copy(filters = filters + other)
            is SimpleFilter -> copy(filters = filters + other)
            else -> this
        }
    override infix fun or(other: Filter) =
        when(other) {
            is AndFilter -> OrFilter(filters = listOf(this, other))
            is OrFilter -> OrFilter(filters = listOf(this, other))
            is SimpleFilter -> OrFilter(filters = listOf(this, other))
            else -> this
        }

    override fun matches(meter: Meter): Boolean = filters.all { it.matches(meter) }
    override fun toString(): String = filters.joinToString(" AND ", prefix = "(", postfix = ")")
    override fun toElasticQuery(): String = buildString {
        append("\"bool\":{\"must\":[")
        append(filters.joinToString(",") { "{${it.toElasticQuery()}}" })
        append("]}")
    }
}
