package com.icure.monitoring.probes

import com.icure.monitoring.probes.dsl.ProbeConfig
import com.icure.monitoring.probes.dsl.collectors.TimeWindowCollector
import com.icure.monitoring.probes.dsl.descriptors.DescriptorElement
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Meter.Id
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.noop.NoopGauge
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Concrete probe that fetches and aggregates data from ElasticSearch using the provided configuration.
 */
@Suppress("UNCHECKED_CAST")
class ElasticProbe(
    private val index: String,
    cron: String,
    config: ProbeConfig
) : SchedulableProbe(cron, config) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        data class DescriptorsWithValue(val descriptors: Set<DescriptorElement>, val value: Double?)

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

    private val client = HttpClient(CIO)
    private val timeWindow = config.collectorProducer().let {
        if (it is TimeWindowCollector) it.samplingDurationMillis
        else throw IllegalArgumentException("Only TimeWindowCollector is supported in probe ${config.probeId}")
    }

    private fun computeQuery() = buildString {
        append(""""bool":{"must":[""")
        val to = System.currentTimeMillis()
        val from = to - timeWindow
        append("""{"range":{"D_timestampedItem-date":{"format":"epoch_millis","gte":"$from","lte": "$to"}}}""")
        append(",{${filter.toElasticQuery()}}")
        append("]}")
    }

    override suspend fun fetchData(elasticUrl: String, elasticUsername: String?, elasticPassword: String?): Set<DescriptorsWithValue>? {
        val url = "$elasticUrl/$index/_search?size=0"
        val descriptors = descriptorsGenerator(NoopGauge(Id(UUID.randomUUID().toString(), Tags.empty(), null, null, Meter.Type.GAUGE)))
            .map { it.k }
        val body = """{"query":{${computeQuery()}},${aggs(descriptors)}}"""
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
            val payload = DEFAULT_JSON.parseToJsonElement(responseAsText) as JsonObject
            return result(payload["aggregations"] as JsonObject, descriptors, emptySet()).toSet()
        } catch (e: Exception) {
            log.warn("Exception occurred while parsing Elasticsearch result (url=$url, body=$body): result=$responseAsText", e)
            null
        }
    }

    private fun aggs(descriptors: List<String>): String {
        return if (descriptors.isEmpty()) {
            """"aggs":{"$id":${aggregator.toElasticAggregation(extractor)}}"""
        } else {
            val descriptor = descriptors.first()
            """"aggs":{"$descriptor":{"terms":{"field":"$descriptor","size":2147483647},${aggs(descriptors.drop(1))}}}"""
        }
    }

    private fun result(payload: JsonObject, inputDescriptors: List<String>, outputDescriptors: Set<DescriptorElement>): List<DescriptorsWithValue> {
        return if (inputDescriptors.isEmpty()) {
            listOf(DescriptorsWithValue(outputDescriptors, ((payload[id] as JsonObject)["value"] as JsonPrimitive).takeUnless { it is JsonNull }?.content?.toDouble()))
        } else {
            val currentInputDescriptor = inputDescriptors.first()
            val innerPayloads = ((payload[currentInputDescriptor] as JsonObject)["buckets"] as List<JsonObject>)
            return innerPayloads.flatMap { innerPayload ->
                val descriptorValue = (innerPayload["key"] as JsonPrimitive).content
                val newOutputDescriptors = outputDescriptors.plus(DescriptorElement(currentInputDescriptor, descriptorValue))
                result(innerPayload, inputDescriptors.drop(1), newOutputDescriptors)
            }
        }
    }
}
