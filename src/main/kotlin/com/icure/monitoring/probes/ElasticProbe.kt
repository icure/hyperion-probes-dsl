package com.icure.monitoring.probes

import com.icure.monitoring.probes.dsl.ProbeConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Concrete probe that fetches and aggregates data from ElasticSearch using the provided configuration.
 */
class ElasticProbe(
    private val index: String,
    cron: String,
    config: ProbeConfig
) : SchedulableProbe(cron, config) {

    companion object {
        @Serializable
        private data class AggregationResult(
            val value: Double
        )

        @Serializable
        private data class Aggregations(
            val aggregations: Map<String, AggregationResult>
        )

        @OptIn(ExperimentalSerializationApi::class)
        private val DEFAULT_JSON: Json = Json {
            encodeDefaults = true
            prettyPrint = false
            isLenient = true
            explicitNulls = false
            ignoreUnknownKeys=true
            coerceInputValues=true
            allowSpecialFloatingPointValues=true
            allowSpecialFloatingPointValues=true
        }
    }

    private val elasticUrl: String = System.getenv("MANAGEMENT_ELASTIC_METRICS_EXPORT_HOST")
    private val elasticUsername: String = System.getenv("MANAGEMENT_ELASTIC_METRICS_EXPORT_USERNAME")
    private val elasticPassword: String = System.getenv("MANAGEMENT_ELASTIC_METRICS_EXPORT_PASSWORD")

    private val client = HttpClient(CIO)

    private fun computeQuery() = buildString {
        append("\"bool\":{\"must\":[")
        val to = System.currentTimeMillis()
        val from = to - trigger.timeFrame.toMillis()
        append("{\"range\":{\"@timestamp\":{\"format\":\"epoch_millis\",\"gte\":\"${from}\",\"lte\": \"${to}\"}}}")
        append(",{${filter.toElasticQuery()}}")
        append("]}")
    }

    override suspend fun fetchData(): Double? {
        val result = client.post("$elasticUrl/$index/_search?size=0") {
            basicAuth(elasticUsername, elasticPassword)
            contentType(ContentType.Application.Json)
            setBody("{\"query\":{${computeQuery()}},\"aggs\":{\"$id\":${trigger.toElasticAggregation()}}}")
        }
        val payload = DEFAULT_JSON.decodeFromString<Aggregations>(result.bodyAsText())
        return payload.aggregations[id]?.value
    }

}