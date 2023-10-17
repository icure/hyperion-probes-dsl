package com.icure.monitoring.probes.dsl

import com.icure.monitoring.probes.dsl.aggregators.Aggregator
import com.icure.monitoring.probes.dsl.aggregators.MaxAggregator
import com.icure.monitoring.probes.dsl.collectors.Collector
import com.icure.monitoring.probes.dsl.data.DataSource
import com.icure.monitoring.probes.dsl.data.DataSourceScope
import com.icure.monitoring.probes.dsl.extractors.Extractor
import com.icure.monitoring.probes.dsl.extractors.ExtractorFactory
import com.icure.monitoring.probes.dsl.filters.Filter
import com.icure.monitoring.probes.dsl.utils.AggregatorParams
import com.icure.monitoring.probes.dsl.utils.ExtractorFactoryParams
import io.micrometer.core.instrument.Meter

open class DataAggregationChain {

    lateinit var definedDataSource: DataSource
    lateinit var definedFilter: Filter
    lateinit var definedAggregator: Aggregator
    lateinit var definedExtractor: Extractor
    lateinit var collectorProducer: () -> Collector

    /**
     * Defines the data source for this Probe. Can be defined only once per configuration.
     */
    @DataSourceScope
    fun dataSource(block: (@DataSourceScope DataSource.Companion).() -> DataSource) {
        definedDataSource = block(DataSource.Companion)
    }

    /**
     * Defines a [Filter] that will select only the [Meter]s that are relevant for the probe.
     */
    fun filter(block: () -> Filter) { definedFilter = block() }

    // region aggregator-configuration
    /**
     * Will aggregate the result using the [ExtractorFactory.forMaxAggregator] version of the [Extractor].
     */
    fun max(block: () -> ExtractorFactoryParams) {
        definedAggregator = MaxAggregator
        val params = block()
        definedExtractor = params.extractor.forMaxAggregator()
        collectorProducer = params.collectorProducer
    }

    /**
     * Will aggregate the result using the [ExtractorFactory.forAverageAggregator] version of the [Extractor].
     */
    fun average(block: () -> ExtractorFactoryParams) {
        definedAggregator = MaxAggregator
        val params = block()
        definedExtractor = params.extractor.forAverageAggregator()
        collectorProducer = params.collectorProducer
    }

    fun customAggregation(block: () -> AggregatorParams) {
        val params = block()
        definedAggregator = params.aggregator
        definedExtractor = params.extractor
        collectorProducer = params.collectorProducer
    }
    // endregion

}