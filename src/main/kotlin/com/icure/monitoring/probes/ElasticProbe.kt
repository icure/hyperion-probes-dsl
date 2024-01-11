package com.icure.monitoring.probes

import com.icure.monitoring.probes.dsl.ProbeConfig
import com.icure.monitoring.probes.dsl.collectors.TimeWindowCollector
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Concrete probe that fetches and aggregates data from ElasticSearch using the provided configuration.
 */
class ElasticProbe(
    private val index: String,
    cron: String,
    config: ProbeConfig
) : SchedulableProbe(cron, config) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        @Serializable
        private data class AggregationResult(
            val `d_gauge-value`: Double
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
    private val elasticUsername: String? = System.getenv("MANAGEMENT_ELASTIC_METRICS_EXPORT_USERNAME")
    private val elasticPassword: String? = System.getenv("MANAGEMENT_ELASTIC_METRICS_EXPORT_PASSWORD")

    private val client = HttpClient(CIO)
    private val timeWindow = config.collectorProducer().let {
        if (it is TimeWindowCollector) it.samplingDurationMillis
        else throw IllegalArgumentException("Only TimeWindowCollector is supported in probe ${config.probeId}")
    }

    private fun computeQuery() = buildString {
        append("\"bool\":{\"must\":[")
        val to = System.currentTimeMillis()
        val from = to - timeWindow
        append("{\"range\":{\"D_timestampedItem-date\":{\"format\":\"epoch_millis\",\"gte\":\"${from}\",\"lte\": \"${to}\"}}}")
        append(",{${filter.toElasticQuery()}}")
        append("]}")
    }

    override suspend fun fetchData(): Double? {
        val url = "$elasticUrl/$index/_search?size=0"
        val body = "{\"query\":{${computeQuery()}},\"aggs\":{\"$id\":${aggregator.toElasticAggregation(extractor)}}}"
        val responseAsText = try {
            val response = client.post(url) {
                if (!elasticUsername.isNullOrBlank() && !elasticPassword.isNullOrBlank()) {
                    basicAuth(elasticUsername, elasticPassword)
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (response.status.isSuccess()) {
                response.bodyAsText()
            } else {
                log.warn("Elasticsearch query failed (url=$url, body=$body): status=${response.status}, response=${response.bodyAsText()}")
                return null
            }
        } catch (e: Exception) {
            log.warn("Elasticsearch query failed (url=$url, body=$body)", e)
            return null
        }
        return try {
            val payload = DEFAULT_JSON.decodeFromString<Aggregations>(responseAsText)
            payload.aggregations[id]?.`d_gauge-value`
        } catch (e: Exception) {
            log.warn("Exception occurred while parsing Elasticsearch result (url=$url, body=$body): result=$responseAsText", e)
            null
        }
    }

}
