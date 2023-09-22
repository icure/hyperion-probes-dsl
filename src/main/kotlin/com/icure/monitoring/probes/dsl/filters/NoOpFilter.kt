package com.icure.monitoring.probes.dsl.filters

import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * A catch-all [Filter] that matches all the [Meter]s.
 * On ElasticSearch, it matches all the documents using the `match_all` operator.
 */
@Serializable
object NoOpFilter : Filter {
    override fun matches(meter: Meter): Boolean = true
    override fun toString(): String = "No filter"
    override infix fun and(other: Filter): Filter = other
    override infix fun or(other: Filter): Filter = this
    override fun toElasticQuery(): String = "\"match_all\":{}"
}