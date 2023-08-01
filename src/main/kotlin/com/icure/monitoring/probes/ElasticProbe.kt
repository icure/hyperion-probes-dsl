package com.icure.monitoring.probes

import com.icure.monitoring.probes.dsl.ProbeConfig
import com.icure.monitoring.utils.Aggregations
import com.icure.monitoring.utils.DEFAULT_JSON
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Concrete probe that fetches and aggregates data from ElasticSearch using the provided configuration.
 */
class ElasticProbe(
    private val index: String,
    cron: String,
    config: ProbeConfig
) : SchedulableProbe(cron, config) {

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