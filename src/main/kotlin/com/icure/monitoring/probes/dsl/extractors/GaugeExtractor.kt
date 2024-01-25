package com.icure.monitoring.probes.dsl.extractors

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter

/**
 * Base class for the hierarchy of [Extractor]s that operate on [Gauge].
 */
abstract class GaugeExtractor : Extractor {
    companion object: ExtractorFactory {
        override fun forMaxAggregator(valueField: String) = GaugeValue(valueField)
        override fun forAverageAggregator(valueField: String) = GaugeValue(valueField)
        override fun forCountAggregator() = GaugeCount()
    }
}

/**
 * Extracts the value from a [Gauge].
 */
class GaugeValue(valueField: String) : GaugeExtractor() {
    override val field: String = "value"
    override val query: String = """{"field":"$valueField"}"""

    companion object: SingleExtractorFactory {
        override fun getExtractor(valueField: String): Extractor = GaugeValue(valueField)
    }

    override fun valueOf(meter: Meter): Double? = if(meter is Gauge) meter.value().takeIf { it.isFinite() } else null
}

/**
 * As [Gauge]s contain only one value, this extractor only returns 1 if the meter is a [Gauge].
 */
class GaugeCount : GaugeExtractor() {
    override val field: String = "value"
    override val query: String = """{"field":"dummyField","missing":1}"""

    companion object: SingleExtractorFactory {
        override fun getExtractor(valueField: String): Extractor = GaugeCount()
    }

    override fun valueOf(meter: Meter): Double? = 1.0.takeIf { meter is Gauge }
}
