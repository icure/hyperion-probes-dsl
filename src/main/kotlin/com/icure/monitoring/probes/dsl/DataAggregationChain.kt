package com.icure.monitoring.probes.dsl

import com.icure.monitoring.probes.dsl.aggregators.Aggregator
import com.icure.monitoring.probes.dsl.aggregators.AverageAggregator
import com.icure.monitoring.probes.dsl.aggregators.MaxAggregator
import com.icure.monitoring.probes.dsl.aggregators.SumAggregator
import com.icure.monitoring.probes.dsl.collectors.Collector
import com.icure.monitoring.probes.dsl.data.DataSource
import com.icure.monitoring.probes.dsl.extractors.Extractor
import com.icure.monitoring.probes.dsl.extractors.ExtractorFactory
import com.icure.monitoring.probes.dsl.filters.Filter
import com.icure.monitoring.probes.dsl.utils.AggregatorParams
import com.icure.monitoring.probes.dsl.utils.ExtractorFactoryParams
import io.micrometer.core.instrument.Meter

@DslMarker
@Target(AnnotationTarget.FUNCTION)
annotation class DataAggregationScope

open class DataAggregationChain {

    lateinit var definedDataSource: DataSource
    lateinit var definedFilter: Filter
    lateinit var definedAggregator: Aggregator
    lateinit var definedExtractor: Extractor
    lateinit var collectorProducer: () -> Collector

    /**
     * Defines the data source for this Probe. Can be defined only once per configuration.
     */
    @DataAggregationScope
    fun dataSource(block: (DataSource.Companion).() -> DataSource) {
        definedDataSource = block(DataSource.Companion)
    }

    /**
     * Defines a [Filter] that will select only the [Meter]s that are relevant for the probe.
     */
    @DataAggregationScope
    fun filter(block: () -> Filter) { definedFilter = block() }

    // region aggregator-configuration
    /**
     * Will aggregate the result using the [ExtractorFactory.forMaxAggregator] version of the [Extractor].
     */
    @DataAggregationScope
    fun max(valueField: String = "value", block: () -> ExtractorFactoryParams) {
        definedAggregator = MaxAggregator
        val params = block()
        definedExtractor = params.extractor.forMaxAggregator(valueField)
        collectorProducer = params.collectorProducer
    }

    /**
     * Will aggregate the result using the [ExtractorFactory.forAverageAggregator] version of the [Extractor].
     */
    @DataAggregationScope
    fun average(valueField: String = "value", block: () -> ExtractorFactoryParams) {
        definedAggregator = AverageAggregator
        val params = block()
        definedExtractor = params.extractor.forAverageAggregator(valueField)
        collectorProducer = params.collectorProducer
    }

    /**
     * Will aggregate the result using the [ExtractorFactory.forCountAggregator] version of the [Extractor].
     */
    @DataAggregationScope
    fun count(block: () -> ExtractorFactoryParams) {
        definedAggregator = SumAggregator
        val params = block()
        definedExtractor = params.extractor.forCountAggregator()
        collectorProducer = params.collectorProducer
    }

    @DataAggregationScope
    fun customAggregation(block: () -> AggregatorParams) {
        val params = block()
        definedAggregator = params.aggregator
        definedExtractor = params.extractor
        collectorProducer = params.collectorProducer
    }
    // endregion

}
