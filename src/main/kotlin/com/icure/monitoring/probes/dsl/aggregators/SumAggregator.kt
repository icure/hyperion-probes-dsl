package com.icure.monitoring.probes.dsl.aggregators

import com.icure.monitoring.probes.dsl.collectors.Collector
import com.icure.monitoring.probes.dsl.collectors.FixedSizeCollector
import com.icure.monitoring.probes.dsl.collectors.TimeWindowCollector
import com.icure.monitoring.probes.dsl.extractors.Extractor
import com.icure.monitoring.probes.dsl.extractors.GaugeExtractor

/**
 * Aggregates the value produced by a [Collector], returning the sum.
 */
object SumAggregator : Aggregator {
    override fun toElasticAggregation(extractor: Extractor): String =
        """{"sum":${extractor.query}}"""

    override fun aggregate(collector: Collector): Double? =
        when(collector) {
            is FixedSizeCollector -> collector.getValues().sum()
            is TimeWindowCollector -> collector.sum()
        }
}
