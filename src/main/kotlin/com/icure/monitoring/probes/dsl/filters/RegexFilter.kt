package com.icure.monitoring.probes.dsl.filters

import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

@Serializable
data class RegexFilter(
    val name: String,
    val pattern: String
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean = meter.id.tags.firstOrNull { it.key == name }?.let {
        Regex(pattern).find(it.value)
    } != null
    override fun toString(): String = "$name matches $pattern"
    override fun toElasticQuery(): String = "\"regexp\":{\"$name\":{\"value\":\"$pattern\"}}"
}

fun matches(name: String, pattern: String) = RegexFilter(name, pattern)
