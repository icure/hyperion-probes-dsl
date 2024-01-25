package com.icure.monitoring.probes.dsl.utils

import com.icure.monitoring.probes.dsl.aggregators.Aggregator
import com.icure.monitoring.probes.dsl.collectors.FixedSizeCollector
import com.icure.monitoring.probes.dsl.collectors.TimeWindowCollector
import com.icure.monitoring.probes.dsl.extractors.Extractor
import com.icure.monitoring.probes.dsl.extractors.ExtractorFactory
import com.icure.monitoring.probes.dsl.extractors.SingleExtractorFactory
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
 * Specifies that the values produced by an [Extractor] should be aggregated on a time window.
 * The first element of the [Pair] passed as parameter defines the duration of the time window, the second defines
 * the sampling interval.
 * @receiver an [ExtractorFactory].
 * @param windowParams the [Pair] of [Duration], [Duration]. The first element defines the duration of the time
 * window, the second the defines the sampling interval.
 * @return a configuration that will be ultimately used to generate a [TimeWindowCollector].
 */
infix fun ExtractorFactory.over(windowParams: Pair<Duration, Duration>) = ExtractorFactoryParams(this) {
    TimeWindowCollector(windowParams.first, windowParams.second)
}

/**
 * Specifies that the values produced by an [Extractor] should be aggregated on a time window.
 * The first element of the [Pair] passed as parameter defines the duration of the time window, the second defines
 * the sampling interval.
 * @receiver an [SingleExtractorFactory].
 * @param windowParams the [Pair] of [Duration], [Duration]. The first element defines the duration of the time
 * window, the second the defines the sampling interval.
 * @return a configuration that will be ultimately used to generate a [TimeWindowCollector].
 */
infix fun SingleExtractorFactory.over(windowParams: Pair<Duration, Duration>) = ExtractorParams(this.getExtractor("value")) {
    TimeWindowCollector(windowParams.first, windowParams.second)
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
