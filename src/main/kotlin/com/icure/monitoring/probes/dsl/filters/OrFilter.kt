package com.icure.monitoring.probes.dsl.filters

import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * Aggregates several [Filter]s through an OR operation.
 * On ElasticSearch, resolves in a boolean query where all the inner filters are in the must statement.
 *
 * @param filters a [List] of [Filter] to combine.
 */
@Serializable
data class OrFilter(
    val filters: List<Filter>
): Filter {
    override infix fun and(other: Filter) =
        when(other) {
            is AndFilter -> AndFilter(filters = listOf(this, other))
            is OrFilter -> AndFilter(filters = listOf(this, other))
            is MatchTagFilter -> AndFilter(filters = listOf(this, other))
            else -> this
        }
    override infix fun or(other: Filter) =
        when(other) {
            is AndFilter -> copy(filters = filters + other)
            is OrFilter -> copy(filters = filters + other.filters)
            is MatchTagFilter -> copy(filters = filters + other)
            else -> this
        }

    override fun matches(meter: Meter): Boolean = filters.any { it.matches(meter) }
    override fun toString(): String = filters.joinToString(" OR ", prefix = "(", postfix = ")")
    override fun toElasticQuery(): String = buildString {
        append("\"bool\":{\"should\":[")
        append(filters.joinToString(",") { "{${it.toElasticQuery()}}" })
        append("]}")
    }
}