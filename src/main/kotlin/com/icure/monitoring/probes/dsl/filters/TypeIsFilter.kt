package com.icure.monitoring.probes.dsl.filters

import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * A [Filter] that matches all the [Meter]s if their type is the one passed as parameter.
 * On ElasticSearch, uses the matches operation.
 *
 * @param type the type of the [Meter] to match.
 */
@Serializable
data class TypeIsFilter(
    val type: Meter.Type
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean = meter.id.type == type
    override fun toString(): String = "type is $type"
    override fun toElasticQuery(): String = "\"match\":{\"type\":\"$type\"}"
}

/**
 * @return a [TypeIsFilter] that matches all the [Meter]s which type is `gauge`.
 */
fun meterIsAGauge() = TypeIsFilter(Meter.Type.GAUGE)

/**
 * @return a [TypeIsFilter] that matches all the [Meter]s which type is `distribution_summary`.
 */
fun meterIsADistribution() = TypeIsFilter(Meter.Type.DISTRIBUTION_SUMMARY)
