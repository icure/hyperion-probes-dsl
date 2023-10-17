package com.icure.monitoring.probes.dsl.utils

import com.icure.monitoring.probes.dsl.collectors.Collector
import com.icure.monitoring.probes.dsl.extractors.Extractor
import com.icure.monitoring.probes.dsl.extractors.ExtractorFactory

data class ExtractorFactoryParams(
    val extractor: ExtractorFactory,
    val collectorProducer: () -> Collector
)

data class ExtractorParams(
    val extractor: Extractor,
    val collectorProducer: () -> Collector
)
