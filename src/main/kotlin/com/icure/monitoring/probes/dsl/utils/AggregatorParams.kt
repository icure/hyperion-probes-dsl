package com.icure.monitoring.probes.dsl.utils

import com.icure.monitoring.probes.dsl.aggregators.Aggregator
import com.icure.monitoring.probes.dsl.collectors.Collector
import com.icure.monitoring.probes.dsl.extractors.Extractor

data class AggregatorParams(
    val aggregator: Aggregator,
    val extractor: Extractor,
    val collectorProducer: () -> Collector
)