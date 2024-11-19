package com.icure.monitoring.probes.dsl.aggregators

import com.icure.monitoring.probes.dsl.collectors.Collector
import com.icure.monitoring.probes.dsl.collectors.FixedSizeCollector
import com.icure.monitoring.probes.dsl.collectors.TimeWindowCollector
import com.icure.monitoring.probes.dsl.extractors.Extractor

/**
 * Aggregates the value produced by a [Collector], returning the maximum.
 */
object MaxAggregator : Aggregator {
    override fun toElasticAggregation(extractor: Extractor): String =
        "{\"max\":{\"field\":\"${extractor.field}\"}}"

    override fun aggregate(collector: Collector): Double? =
        when(collector) {
            is FixedSizeCollector -> collector.getValues().maxOrNull()
            is TimeWindowCollector -> collector.max()
        }
}
