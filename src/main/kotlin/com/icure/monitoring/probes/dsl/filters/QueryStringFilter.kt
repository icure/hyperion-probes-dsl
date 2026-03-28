package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.exceptions.UnsupportedDataSourceException
import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * This [Filter] can filter [Meter]s based on an Elasticsearch query_string.
 * Note: this can only be used on remote ES data sources and will throw an error if applied to registry data sources.
 *
 * @param query the ES query_string
 */
@Serializable
data class QueryStringFilter(
    val query: String
) : SimpleFilter() {

    override fun matches(meter: Meter): Boolean {
        throw UnsupportedDataSourceException("This filter is not compatible with a registry datasource.")
    }
    override fun toString(): String = query
    override fun toElasticQuery(): String = """"query_string":{"query":"${query.replace("\\", "\\\\").replace("\"", "\\\"")}"}"""
}

/**
 * Generates a [QueryStringFilter] for the query passed as parameter.
 *
 * @param query the query_string to match
 * @return a [QueryStringFilter]
 */
fun queryString(query: String) = QueryStringFilter(query)
