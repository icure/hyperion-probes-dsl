package com.icure.monitoring.probes.dsl.extractors

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Meter

/**
 * Base class for the hierarchy of [Extractor]s that operate on [DistributionSummary].
 */
sealed class DistributionSummaryExtractor : Extractor {
    companion object: ExtractorFactory {
        override fun forMaxAggregator(valueField: String) = MaxOfDistributionSummary()
        override fun forAverageAggregator(valueField: String) = AverageOfDistributionSummary()
        override fun forCountAggregator(valueField: String) = CountOfDistributionSummary()
    }
}

/**
 * Extracts the maximum from a [DistributionSummary].
 */
class MaxOfDistributionSummary : DistributionSummaryExtractor() {

    companion object: SingleExtractorFactory {
        override fun getExtractor(valueField: String): Extractor = MaxOfDistributionSummary()
    }

    override val field = "max"
    override fun valueOf(meter: Meter): Double? = if(meter is DistributionSummary) meter.takeSnapshot().max() else null
}

/**
 * Extracts the average value from a [DistributionSummary].
 */
class AverageOfDistributionSummary : DistributionSummaryExtractor() {

    companion object: SingleExtractorFactory {
        override fun getExtractor(valueField: String): Extractor = AverageOfDistributionSummary()
    }


    override val field = "mean"
    override fun valueOf(meter: Meter): Double? = if(meter is DistributionSummary) meter.takeSnapshot().mean() else null
}

/**
 * Extracts the number of values registered in a [DistributionSummary].
 */
class CountOfDistributionSummary : DistributionSummaryExtractor() {

    companion object: SingleExtractorFactory {
        override fun getExtractor(valueField: String): Extractor = CountOfDistributionSummary()
    }

    override val field = "count"
    override fun valueOf(meter: Meter): Double? = if(meter is DistributionSummary) meter.takeSnapshot().count().toDouble() else null
}
