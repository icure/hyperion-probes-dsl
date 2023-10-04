package com.icure.monitoring.probes.dsl.extractors

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter

/**
 * Extracts the value from a [Gauge].
 */
class GaugeValue : Extractor {
    override val field: String = "value"

    companion object: ExtractorFactory {
        override fun forMaxAggregator() = GaugeValue()
        override fun forAverageAggregator() = GaugeValue()
        override fun forCountAggregator() = GaugeCount()
    }
    override fun value(meter: Meter): Double? = if(meter is Gauge) meter.value().takeIf { it.isFinite() } else null

}

/**
 * As [Gauge]s contain only one value, this extractor only returns 1 if the meter is a [Gauge].
 */
class GaugeCount : Extractor {

    override val field: String = "value"
    override fun value(meter: Meter): Double? = 1.0.takeIf { meter is Gauge }
}