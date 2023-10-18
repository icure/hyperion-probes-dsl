package com.icure.monitoring.probes.dsl.aggregators

import com.icure.monitoring.exceptions.UnsupportedDataSourceException
import com.icure.monitoring.probes.dsl.collectors.Collector
import com.icure.monitoring.probes.dsl.extractors.Extractor

/**
 * An [Aggregator] is a component that takes a [Collector] as input and produce a single [Double] value, based on the
 * concrete implementation of the interface.
 */
sealed interface Aggregator {

    /**
     * Generates an ElasticSearch aggregation query based on an [Extractor], that will define which the field of the
     * document to use.
     * Note: this method will always throw an exception on [CustomAggregator].
     *
     * @param extractor the [Extractor].
     * @return an ElasticSearch aggregation in JSON format.
     */
    fun toElasticAggregation(extractor: Extractor): String

    /**
     * @param collector a [Collector] containing the data to aggregate.
     * @return the aggregated value.
     */
    fun aggregate(collector: Collector): Double?
}

/**
 * A utility function to easily define a [CustomAggregator].
 */
fun aggregator(block: (collector: Collector) -> Double) = CustomAggregator(block)

class CustomAggregator(
    private val aggregationFunction: (collector: Collector) -> Double
) : Aggregator {

    override fun toElasticAggregation(extractor: Extractor): String =
        throw UnsupportedDataSourceException("This aggregator is not compatible with a remote ES datasource.")

    override fun aggregate(collector: Collector): Double = aggregationFunction(collector)
}