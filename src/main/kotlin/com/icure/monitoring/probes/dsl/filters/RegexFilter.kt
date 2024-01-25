package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.exceptions.UnsupportedDataSourceException
import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

@Serializable
data class RegexFilter(
    val name: String,
    val pattern: String
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean {
        throw UnsupportedDataSourceException("This filter is not compatible with a registry datasource.")
    }
    override fun toString(): String = "$name matches $pattern"
    override fun toElasticQuery(): String = "\"regexp\":{\"$name\":{\"value\":\"$pattern\"}}"
}

fun matches(name: String, pattern: String) = RegexFilter(name, pattern)
