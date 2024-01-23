package com.icure.monitoring.probes.dsl.extractors

import com.icure.monitoring.exceptions.UnsupportedDataSourceException
import io.micrometer.core.instrument.Meter

/**
 * An [Extractor] is a component of the probe that returns the value of a [Meter] according to the criteria defined
 * by its concrete implementations.
 */
sealed interface Extractor {
    /**
     * A descriptor of the field to use in ElasticSearch aggregations.
     */
    val field: String

    val query: String

    /**
     * Extracts the metrics from a [Meter].
     *
     * @param meter the [Meter] to extract the value from.
     * @return the value of the [Meter] as a [Double] or null.
     */
    fun valueOf(meter: Meter): Double?
}

/**
 * Factory method that instantiates different implementation of an [Extractor] based on the aggregator to apply.
 */
interface ExtractorFactory {
    fun forMaxAggregator(valueField: String): Extractor
    fun forAverageAggregator(valueField: String): Extractor
    fun forCountAggregator(): Extractor
}

/**
 * An interface that instantiate a single implementation of an [Extractor]. It is useful to keep the DSL coherent.
 */
interface SingleExtractorFactory {
    fun getExtractor(valueField: String): Extractor
}

/**
 * Utility function to easily define a [CustomExtractor].
 *
 * @param block the extraction logic, a function that takes a [Meter] as input and return a [Double] if a value can be
 * extracted form that meter.
 * @return a [CustomExtractor].
 */
fun extractor(block: (meter: Meter) -> Double?) = CustomExtractor(block)

/**
 * [Extractor] that implements a custom logic.
 * Note: this can only be used with a registry datasource.
 */
class CustomExtractor(
    private val extractorFunction: (meter: Meter) -> Double?
) : Extractor {

    override val field: String
        get() = throw UnsupportedDataSourceException("This extractor is not compatible with a remote ES datasource.")

    override val query: String
        get() = throw UnsupportedDataSourceException("This extractor is not compatible with a remote ES datasource.")

    override fun valueOf(meter: Meter): Double? = extractorFunction(meter)
}
