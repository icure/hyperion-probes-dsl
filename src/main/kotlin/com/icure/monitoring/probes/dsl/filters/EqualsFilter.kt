package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.exceptions.UnsupportedDataSourceException
import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

@Serializable
data class EqualsFilter(
    val name: String,
    val value: String
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean {
        throw UnsupportedDataSourceException("This filter is not compatible with a registry datasource.")
    }
    override fun toString(): String = "$name is $value"
    override fun toElasticQuery(): String = "\"term\":{\"$name\":\"$value\"}"
}

fun isEqualTo(name: String, value: String) = EqualsFilter(name, value)
