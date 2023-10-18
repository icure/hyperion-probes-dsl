package com.icure.monitoring.probes.dsl.aggregators

import com.icure.monitoring.probes.dsl.collectors.Collector
import com.icure.monitoring.probes.dsl.collectors.FixedSizeCollector
import com.icure.monitoring.probes.dsl.collectors.TimeWindowCollector
import com.icure.monitoring.probes.dsl.extractors.Extractor

/**
 * Aggregates the value produced by a [Collector], returning the average of the values.
 */
object AverageAggregator : Aggregator {
    override fun toElasticAggregation(extractor: Extractor): String =
        "{\"avg\":{\"field\":\"${extractor.field}\"}}"

    override fun aggregate(collector: Collector): Double? = when(collector) {
        is FixedSizeCollector -> collector.getValues().takeIf { it.isNotEmpty() }?.average()
        is TimeWindowCollector -> collector.average()
    }
}