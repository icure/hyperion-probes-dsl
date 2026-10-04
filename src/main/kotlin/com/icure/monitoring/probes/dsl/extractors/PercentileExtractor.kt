package com.icure.monitoring.probes.dsl.extractors

import io.micrometer.core.instrument.DistributionSummary

fun percentileExtractor(percentile: Double): Extractor = extractor { meter ->
	if(meter is DistributionSummary) {
		meter.takeSnapshot().percentileValues().firstNotNullOfOrNull {
			if (it.percentile() == percentile) {
				it.value()
			} else {
				null
			}
		}
	} else {
		null
	}
}