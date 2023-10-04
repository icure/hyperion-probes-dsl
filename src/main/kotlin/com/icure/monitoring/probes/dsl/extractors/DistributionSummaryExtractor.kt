package com.icure.monitoring.probes.dsl.extractors

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Meter

/**
 * Base class for the hierarchy of [Extractor]s that operate on [DistributionSummary].
 */
sealed class DistributionSummaryExtractor : Extractor {
    companion object: ExtractorFactory {
        override fun forMaxAggregator() = MaxOfDistributionSummary()
        override fun forAverageAggregator() = AverageOfDistributionSummary()
        override fun forCountAggregator() = CountOfDistributionSummary()
    }
}

/**
 * Extracts the maximum from a [DistributionSummary].
 */
class MaxOfDistributionSummary : DistributionSummaryExtractor() {
    override val field = "max"
    override fun value(meter: Meter): Double? = if(meter is DistributionSummary) meter.takeSnapshot().max() else null
}

/**
 * Extracts the average value from a [DistributionSummary].
 */
class AverageOfDistributionSummary : DistributionSummaryExtractor() {
    override val field = "mean"
    override fun value(meter: Meter): Double? = if(meter is DistributionSummary) meter.takeSnapshot().mean() else null
}

/**
 * Extracts the number of values registered in a [DistributionSummary].
 */
class CountOfDistributionSummary : DistributionSummaryExtractor() {
    override val field = "count"
    override fun value(meter: Meter): Double? = if(meter is DistributionSummary) meter.takeSnapshot().count().toDouble() else null
}