package com.icure.monitoring.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AggregationResult(
    val value: Double
)

@Serializable
data class Aggregations(
    val aggregations: Map<String, AggregationResult>
)

@OptIn(ExperimentalSerializationApi::class)
val DEFAULT_JSON: Json = Json {
    encodeDefaults = true
    prettyPrint = false
    isLenient = true
    explicitNulls = false
    ignoreUnknownKeys=true
    coerceInputValues=true
    allowSpecialFloatingPointValues=true
    allowSpecialFloatingPointValues=true
}