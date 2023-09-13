package com.icure.monitoring.probes.dsl

import com.icure.monitoring.model.MetricsTags
import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class FilterScope

/**
 * Base interface for all the filters used in the probe DSL
 */
@Serializable
sealed interface Filter {

    /**
     * Checks if the filter matches a [Meter] based on its id.
     */
    fun matches(meter: Meter.Id): Boolean

    /**
     * Combines two [Filter] through an AND operation.
     */
    infix fun and(other: Filter): Filter

    /**
     * Combines two [Filter] through an OR operation.
     */
    infix fun or(other: Filter): Filter

    /**
     * Transforms the current filter to a query compatible with ElasticSearch.
     */
    fun toElasticQuery(): String
}

/**
 * Catch all filter that matches all the meters and all the documents on ElasticSearch.
 */
@Serializable
object NoOpFilter : Filter {
    override fun matches(meter: Meter.Id): Boolean = true
    override fun toString(): String = "No filter"
    override infix fun and(other: Filter): Filter = other
    override infix fun or(other: Filter): Filter = this

    override fun toElasticQuery(): String = "\"match_all\":{}"
}

/**
 * Aggregates several filters through an AND operation.
 * On ElasticSearch, resolves in a boolean query where all the inner filters are in the must statement.
 */
@Serializable
data class AndFilter(
    val filters: List<Filter>
) : Filter {
    override infix fun and(other: Filter) =
        when(other) {
            is AndFilter -> copy(filters = filters + other.filters)
            is OrFilter -> copy(filters = filters + other)
            is MatchTagFilter -> copy(filters = filters + other)
            else -> this
        }
    override infix fun or(other: Filter) =
        when(other) {
            is AndFilter -> OrFilter(filters = listOf(this, other))
            is OrFilter -> OrFilter(filters = listOf(this, other))
            is MatchTagFilter -> OrFilter(filters = listOf(this, other))
            else -> this
        }

    override fun matches(meter: Meter.Id): Boolean = filters.all { it.matches(meter) }
    override fun toString(): String = filters.joinToString(" AND ", prefix = "(", postfix = ")")
    override fun toElasticQuery(): String = buildString {
        append("\"bool\":{\"must\":[")
        append(filters.joinToString(",") { "{${it.toElasticQuery()}}" })
        append("]}")
    }
}

/**
 * Aggregates several filters through an OR operation.
 * On ElasticSearch, resolves in a boolean query where all the inner filters are in the should statement.
 */
@Serializable
data class OrFilter(
    val filters: List<Filter>
): Filter {
    override infix fun and(other: Filter) =
        when(other) {
            is AndFilter -> AndFilter(filters = listOf(this, other))
            is OrFilter -> AndFilter(filters = listOf(this, other))
            is MatchTagFilter -> AndFilter(filters = listOf(this, other))
            else -> this
        }
    override infix fun or(other: Filter) =
        when(other) {
            is AndFilter -> copy(filters = filters + other)
            is OrFilter -> copy(filters = filters + other.filters)
            is MatchTagFilter -> copy(filters = filters + other)
            else -> this
        }

    override fun matches(meter: Meter.Id): Boolean = filters.any { it.matches(meter) }
    override fun toString(): String = filters.joinToString(" OR ", prefix = "(", postfix = ")")
    override fun toElasticQuery(): String = buildString {
        append("\"bool\":{\"should\":[")
        append(filters.joinToString(",") { "{${it.toElasticQuery()}}" })
        append("]}")
    }
}

/**
 * Base interface for all the filters that are not aggregated
 */
@Serializable
sealed class SimpleFilter : Filter {
    override infix fun and(other: Filter) =
        when(other) {
            is AndFilter -> AndFilter(filters = other.filters + this)
            is OrFilter -> AndFilter(filters = listOf(this, other))
            is SimpleFilter -> AndFilter(filters = listOf(this, other))
            else -> this
        }
    override infix fun or(other: Filter) =
        when(other) {
            is AndFilter -> OrFilter(filters = listOf(this, other))
            is OrFilter -> OrFilter(filters = other.filters + this)
            is SimpleFilter -> OrFilter(filters = listOf(this, other))
            else -> this
        }
}

/**
 * A filters that matches all the tags with the specified value.
 * On ElasticSearch, uses the match operation.
 */
@Serializable
data class MatchTagFilter(
    val tag: MetricsTags,
    val matchValue: String
) : SimpleFilter() {

    override fun matches(meter: Meter.Id): Boolean = meter.tags.firstOrNull { it.key == tag.tagName }?.let {
        Regex(matchValue).find(it.value)
    } != null
    override fun toString(): String = "${tag.tagName} matches $matchValue"
    override fun toElasticQuery(): String = "\"match\":{\"${tag.queryValue}\":\"$matchValue\"}"
}

/**
 * A filters that matches the metrics with the specified name.
 * On ElasticSearch, uses the match operation.
 */
@Serializable
data class MatchNameFilter(
    val query: String
) : SimpleFilter() {

    override fun matches(meter: Meter.Id): Boolean = meter.name == query
    override fun toString(): String = "name matches $query"
    override fun toElasticQuery(): String = "\"match\":{\"name\":\"$query\"}"
}

/**
 * A filters that matches the metrics using a regex on a field.
 * On ElasticSearch, uses the regexp operation.
 */
@Serializable
data class NameRegexFilter(
    val pattern: String
) : SimpleFilter() {

    override fun matches(meter: Meter.Id): Boolean = Regex(pattern).containsMatchIn(meter.name)
    override fun toString(): String = "name matches $pattern"
    override fun toElasticQuery(): String = "\"regexp\":{\"name\":{\"value\":\"$pattern\"}}"
}

/**
 * A filters that matches the type of metric.
 * On ElasticSearch, uses the matches operation.
 */
@Serializable
data class TypeMatchFilter(
    val type: String
) : SimpleFilter() {

    override fun matches(meter: Meter.Id): Boolean = meter.type.toString().lowercase() == type
    override fun toString(): String = "type is $type"
    override fun toElasticQuery(): String = "\"match\":{\"type\":\"$type\"}"
}

infix fun MetricsTags.matches(value: String) = MatchTagFilter(this, value)
fun metricNameIs(query: String) = MatchNameFilter(query)
fun metricNameMatches(pattern: String) = NameRegexFilter(pattern)
fun metricIsAGauge() = TypeMatchFilter("gauge")
fun metricIsADistribution() = TypeMatchFilter("distribution_summary")