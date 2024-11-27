package com.icure.monitoring.probes.dsl.utils

import com.icure.monitoring.probes.dsl.aggregators.Aggregator
import com.icure.monitoring.probes.dsl.collectors.FixedSizeCollector
import com.icure.monitoring.probes.dsl.collectors.TimeWindowCollector
import com.icure.monitoring.probes.dsl.extractors.Extractor
import com.icure.monitoring.probes.dsl.extractors.ExtractorFactory
import com.icure.monitoring.probes.dsl.extractors.SingleExtractorFactory
import io.micrometer.core.instrument.Clock
import java.time.Duration

/**
 * Specifies that the values produced by an [Extractor] should be aggregated on a time window of the specified duration
 * with a sampling time of 60 seconds.
 * @receiver an [ExtractorFactory].
 * @param duration the [Duration] of the time window.
 * @return a configuration that will be ultimately used to generate a [TimeWindowCollector].
 */
infix fun ExtractorFactory.over(duration: Duration) = ExtractorFactoryParams(this) {
    TimeWindowCollector(duration, Duration.ofSeconds(60))
}

/**
 * Specifies that the values produced by an [Extractor] should be aggregated on a time window of the specified duration
 * with a sampling time of 60 seconds.
 * @receiver an [SingleExtractorFactory].
 * @param duration the [Duration] of the time window.
 * @return a configuration that will be ultimately used to generate a [TimeWindowCollector].
 */
infix fun SingleExtractorFactory.over(duration: Duration) = ExtractorParams(this.getExtractor("value")) {
    TimeWindowCollector(duration, Duration.ofSeconds(60))
}

/**
 * Specifies that the values produced by an [Extractor] should be aggregated specifying all the parameters of the sampling
 * window
 * @receiver an [ExtractorFactory].
 * @param parameters an instance of [SamplingParameters].
 * @return a configuration that will be ultimately used to generate a [TimeWindowCollector].
 */
infix fun ExtractorFactory.sampledWith(parameters: SamplingParameters) = ExtractorFactoryParams(this) {
    TimeWindowCollector(
        timeFrame = parameters.timeFrame,
        samplingDuration = parameters.samplingDuration,
        clock = parameters.clock,
        lowestValue = parameters.lowestValue,
        highestValue = parameters.highestValue,
        significantDigits = parameters.significantDigits
    )
}

/**
 * Specifies that the values produced by an [Extractor] should be aggregated specifying all the parameters of the sampling
 * window
 * @receiver an [ExtractorFactory].
 * @param parameters an instance of [SamplingParameters].
 * @return a configuration that will be ultimately used to generate a [TimeWindowCollector].
 */
infix fun SingleExtractorFactory.sampledWith(parameters: SamplingParameters) =  ExtractorParams(this.getExtractor("value")) {
    TimeWindowCollector(
        timeFrame = parameters.timeFrame,
        samplingDuration = parameters.samplingDuration,
        clock = parameters.clock,
        lowestValue = parameters.lowestValue,
        highestValue = parameters.highestValue,
        significantDigits = parameters.significantDigits
    )
}

/**
 * Specifies that the aggregation should take into account the last N values produced by the [Extractor].
 * @receiver the number of values to take into account.
 * @param extractor an [ExtractorFactory].
 * @return a configuration that will be ultimately used to generate a [FixedSizeCollector].
 */
infix fun Int.lastProducedBy(extractor: ExtractorFactory) = ExtractorFactoryParams(extractor) {
    FixedSizeCollector(this)
}

/**
 * Specifies that the aggregation should take into account the last N values produced by the [Extractor].
 * @receiver the number of values to take into account.
 * @param extractor an [SingleExtractorFactory].
 * @return a configuration that will be ultimately used to generate a [FixedSizeCollector].
 */
infix fun Int.lastProducedBy(extractor: SingleExtractorFactory) = ExtractorParams(extractor.getExtractor("value")) {
    FixedSizeCollector(this)
}

/**
 * @receiver some [ExtractorParams].
 * @param aggregator an [Aggregator].
 * @return a configuration object that can be used to set up a chain of aggregation based on those params.
 */
infix fun ExtractorParams.aggregateUsing(aggregator: Aggregator) =
    AggregatorParams(aggregator, extractor, collectorProducer)

/**
 * Parameters to further specify the sampling behaviour of a [TimeWindowCollector].
 * @param timeFrame the duration of the sampling time window. All the samples older than now - this value will be deleted.
 * @param samplingDuration the duration of a sampling bucket. All the samples received in a window of this size will be
 * aggregated together.
 * @param clock an implementation of [Clock]. The default value uses the system clock. You normally needto change this
 * only for testing.
 * @param lowestValue the lower bound of the underlying histogram implementation used to collect the samples.
 * @param highestValue the upper bound of the underlying histogram implementation used to collect the samples.
 * @param significantDigits the precision of the buckets of the underlying histogram implementation. It goes from 0 to 5.
 * The highest the number, the more digits will be preserved
 */
data class SamplingParameters(
    val timeFrame: Duration,
    val samplingDuration: Duration = Duration.ofSeconds(60),
    val clock: Clock = Clock.SYSTEM,
    val lowestValue: Long = 10,
    val highestValue: Long = 1_000_000_000,
    val significantDigits: Int = 2,
)