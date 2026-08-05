package com.icure.monitoring.probes

import com.icure.monitoring.probes.dsl.ProbeConfig
import com.icure.monitoring.probes.dsl.collectors.TimeWindowCollector
import com.icure.monitoring.probes.dsl.descriptors.DescriptorElement
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
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
		/**
         * Bucket key assigned to documents that lack a grouping field. Without `missing`, an ES
         * `terms` aggregation silently drops any document that has no value for the bucketed field,
         * so a probe grouping by e.g. `namespace`/`replica_id`/`service_name` never sees the
         * documents that legitimately don't carry those tags (node-level disks, node/pod network
         * counters, …) — and therefore never fires for them. Bucketing them under this key keeps
         * them in the result instead of losing them.
         */
        const val MISSING_BUCKET_KEY = "__NA__"

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

	private val client = HttpClient(CIO) {
		install(HttpTimeout) {
			requestTimeoutMillis = 30_000
			connectTimeoutMillis = 5_000
			socketTimeoutMillis = 15_000
		}
	}
	private val timeWindow = config.collectorProducer().let {
		if (it is TimeWindowCollector) it.timeFrame.toMillis()
		else throw IllegalArgumentException("Only TimeWindowCollector is supported in probe ${config.probeId}")
	}

	private fun computeQuery() = buildString {
		append(""""bool":{"must":[""")
		val to = System.currentTimeMillis()
		val from = to - timeWindow
		append("""{"range":{"$timestampField":{"format":"epoch_millis","gte":"$from","lte": "$to"}}}""")
		append(",{${filter.toElasticQuery()}}")
		append("]}")
	}

	override suspend fun fetchData(elasticUrl: String, elasticUsername: String?, elasticPassword: String?, elasticHeaders: Map<String, String>?): Set<DescriptorsWithValue>? {
		val url = "$elasticUrl/$index/_search?size=0"
		val descriptors = descriptorsGenerator(NoopGauge(Id(UUID.randomUUID().toString(), Tags.empty(), null, null, Meter.Type.GAUGE)))
			.map { it.k }
		val body = """{"query":{${computeQuery()}},${aggs(descriptors)}}"""
		val responseAsText = try {
			val response = client.post(url) {
				if (!elasticUsername.isNullOrBlank() && !elasticPassword.isNullOrBlank()) {
					basicAuth(elasticUsername, elasticPassword)
				}
				elasticHeaders?.forEach { (key, value) ->
					header(key, value)
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
			val aggregations = payload["aggregations"] as? JsonObject
			if (aggregations == null) {
				log.warn("No aggregations in ES response (shard failures?): url=$url, result=$responseAsText")
				return null
			}
			result(aggregations, descriptors, emptySet()).toSet()
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
			// `missing` keeps documents that lack this field instead of silently dropping them (see MISSING_BUCKET_KEY).
            """"aggs":{"$descriptor":{"terms":{"field":"$descriptor","size":2147483647,"missing":"$MISSING_BUCKET_KEY"},${aggs(descriptors.drop(1))}}}"""
		}
	}

	private fun result(payload: JsonObject, inputDescriptors: List<String>, outputDescriptors: Set<DescriptorElement>): List<DescriptorsWithValue> {
		return if (inputDescriptors.isEmpty()) {
			listOf(DescriptorsWithValue(outputDescriptors, ((payload[id] as JsonObject)["value"] as JsonPrimitive).takeUnless { it is JsonNull }?.content?.toDouble()))
		} else {
			val currentInputDescriptor = inputDescriptors.first()
			val innerPayloads = ((payload[currentInputDescriptor] as JsonObject)["buckets"] as List<JsonObject>)
			innerPayloads.flatMap { innerPayload ->
				val descriptorValue = (innerPayload["key"] as JsonPrimitive).content
				val newOutputDescriptors = outputDescriptors.plus(DescriptorElement(currentInputDescriptor, descriptorValue))
				result(innerPayload, inputDescriptors.drop(1), newOutputDescriptors)
			}
		}
	}
}
