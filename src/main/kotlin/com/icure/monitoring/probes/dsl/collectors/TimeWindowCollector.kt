package com.icure.monitoring.probes.dsl.collectors

import com.dynatrace.dynahist.layout.LogQuadraticLayout
import com.icure.monitoring.meters.HistogramBucket
import io.micrometer.core.instrument.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration

/**
 * This collector aggregates the result over a fixed timeframe, sampling them using the duration passed as parameter.
 * To collect a large quantity of data while keeping the memory consumption low, the implementation relies on
 * [com.dynatrace.dynahist.Histogram]. This means that it is not possible to retrieve the exact values registered in the
 * collector, but just an approximation, that consists in publishing to the list of value N time the average value of
 * each bin of each histogram, where N is the count of values registered to the histogram.
 * Under this assumption, the error of the approximation depends on the absolute and relative bin width.
 * For more details, check [Dynahist repo](https://github.com/dynatrace-oss/dynahist#bin-layouts)
 *
 * @param timeFrame the [Duration] of the accumulation window. Only the samples in this time frame will be
 * considered and the older will be automatically removed.
 * @param samplingDuration the sampling window [Duration]. All the samples with the same window id (calculated as
 * currentTimestamp/samplingDuration) will be aggregated together.
 * @param clock an instance of [Clock] to calculate the timings. The default implementation is based on
 * [System.currentTimeMillis].
 */
class TimeWindowCollector(
    timeFrame: Duration,
    samplingDuration: Duration,
    private val clock: Clock = Clock.SYSTEM
) : Collector {

    private val sampledTimeFrame = timeFrame.toMillis() / samplingDuration.toMillis()
    private val samplingDurationMillis = samplingDuration.toMillis()
    private val layout = LogQuadraticLayout.create(10.0, 1e-2, 0.0, 1e9)
    private val buckets = mutableMapOf<Long, HistogramBucket>()
    private val bucketsMutex = Mutex()

    override suspend fun addValue(value: Double) {
        bucketsMutex.withLock {
            val currentIndex = clock.wallTime() / samplingDurationMillis
            buckets.getOrPut(currentIndex) {
                HistogramBucket.create(layout)
            }.addValue(value)
            buckets.forEach { (k, _) ->
                if(k < (currentIndex - sampledTimeFrame)) {
                    buckets.remove(k)
                }
            }
        }
    }

    private fun getBucketsInTimeWindow() = (clock.wallTime() / samplingDurationMillis).let { currentIndex ->
        buckets.filterKeys { k ->
            k >= (currentIndex - sampledTimeFrame)
        }.values
    }

    override fun getValues(): List<Double> = getBucketsInTimeWindow().flatMap { bucket ->
        bucket.histogram.nonEmptyBinsAscending().flatMap { bin ->
            List(bin.binCount.toInt()) {
                (bin.upperBound + bin.lowerBound) / 2.0
            }
        }
    }

    /**
     * @return the maximum value registered in the time window. Note: differently from the values returned by
     * [getValues], this is NOT approximated.
     */
    fun max(): Double = getBucketsInTimeWindow().maxOf { it.max }
}