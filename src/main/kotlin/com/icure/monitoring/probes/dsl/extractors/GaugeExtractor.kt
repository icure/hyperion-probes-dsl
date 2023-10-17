package com.icure.monitoring.probes.dsl.extractors

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter

/**
 * Base class for the hierarchy of [Extractor]s that operate on [Gauge].
 */
sealed class GaugeExtractor : Extractor {
    companion object: ExtractorFactory {
        override fun forMaxAggregator() = GaugeValue()
        override fun forAverageAggregator() = GaugeValue()
        override fun forCountAggregator() = GaugeCount()
    }
}

/**
 * Extracts the value from a [Gauge].
 */
class GaugeValue : GaugeExtractor() {
    override val field: String = "value"

    companion object: SingleExtractorFactory {
        override fun getExtractor(): Extractor = GaugeValue()
    }
    override fun valueOf(meter: Meter): Double? = if(meter is Gauge) meter.value().takeIf { it.isFinite() } else null

}

/**
 * As [Gauge]s contain only one value, this extractor only returns 1 if the meter is a [Gauge].
 */
class GaugeCount : GaugeExtractor() {

    companion object: SingleExtractorFactory {
        override fun getExtractor(): Extractor = GaugeCount()
    }

    override val field: String = "value"
    override fun valueOf(meter: Meter): Double? = 1.0.takeIf { meter is Gauge }
}