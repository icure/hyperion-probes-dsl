package com.icure.monitoring.probes.dsl.filters

import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * Base interface for all the filters used in the probe DSL
 */
@Serializable
sealed interface Filter {

    /**
     * Checks if the filter matches a [Meter].
     */
    fun matches(meter: Meter): Boolean

    /**
     * Combines two [Filter] through an AND operation.
     */
    infix fun and(other: Filter): Filter

    /**
     * Combines two [Filter] through an OR operation.
     */
    infix fun or(other: Filter): Filter

    /**
     * Transforms the current filter to a query compatible with ElasticSearch.
     */
    fun toElasticQuery(): String
}

/**
 * Base interface for all the [Filter]s that are not an aggregation of other filters
 */
@Serializable
sealed class SimpleFilter : Filter {
    override infix fun and(other: Filter) =
        when(other) {
            is AndFilter -> AndFilter(filters = other.filters + this)
            is OrFilter -> AndFilter(filters = listOf(this, other))
            is SimpleFilter -> AndFilter(filters = listOf(this, other))
            else -> this
        }
    override infix fun or(other: Filter) =
        when(other) {
            is AndFilter -> OrFilter(filters = listOf(this, other))
            is OrFilter -> OrFilter(filters = other.filters + this)
            is SimpleFilter -> OrFilter(filters = listOf(this, other))
            else -> this
        }
}
