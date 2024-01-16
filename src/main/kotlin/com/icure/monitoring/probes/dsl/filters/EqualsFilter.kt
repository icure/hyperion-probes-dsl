package com.icure.monitoring.probes.dsl.filters

import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

@Serializable
data class EqualsFilter(
    val name: String,
    val value: String
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean = meter.id.tags.firstOrNull { it.key == name }?.let {
        value == it.value
    } != null
    override fun toString(): String = "$name is $value"
    override fun toElasticQuery(): String = "\"term\":{\"$name\":\"$value\"}"
}

fun isEqualTo(name: String, value: String) = EqualsFilter(name, value)
