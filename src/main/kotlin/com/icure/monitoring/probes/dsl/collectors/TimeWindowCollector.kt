package com.icure.monitoring.probes.dsl.collectors

import com.icure.monitoring.meters.HistogramBucket
import io.micrometer.core.instrument.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration

/**
 * This collector aggregates the result over a fixed timeframe, sampling them using the duration passed as parameter.
 * To collect a large quantity of data while keeping the memory consumption low, the implementation relies on
 * [org.HdrHistogram.Histogram]. This means that it is not possible to retrieve the exact values registered in the
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
    val timeFrame: Duration,
    samplingDuration: Duration,
    private val clock: Clock = Clock.SYSTEM,
    private val lowestValue: Long = 10,
    private val highestValue: Long = 1_000_000_000,
    private val significantDigits: Int = 2,
) : Collector {

    private val instantiationTime = clock.wallTime()
    private val sampledTimeFrame = timeFrame.toMillis() / samplingDuration.toMillis()
    private val samplingDurationMillis = samplingDuration.toMillis()
    private val buckets = mutableMapOf<Long, HistogramBucket>()
    private val bucketsMutex = Mutex()

    override suspend fun addValue(value: Double) {
        bucketsMutex.withLock {
            val currentIndex = clock.wallTime() / samplingDurationMillis
            buckets.getOrPut(currentIndex) {
                HistogramBucket.create(
                    lowestValue = lowestValue,
                    highestValue = highestValue,
                    significantDigits = significantDigits
                )
            }.addValue(value)
            val indicesToRemove = buckets.mapNotNull { (k, _) ->
                if(k < (currentIndex - sampledTimeFrame)) k
                else null
            }
            indicesToRemove.forEach {
                buckets.remove(it)
            }

        }
    }

    private fun getBucketsInTimeWindow() =
        // If a whole timeframe has not passed yet, my results are incomplete
        if((clock.wallTime() - instantiationTime) >= timeFrame.toMillis()) {
            (clock.wallTime() / samplingDurationMillis).let { currentIndex ->
                buckets.filterKeys { k ->
                    k >= (currentIndex - sampledTimeFrame)
                }.values
            }
        } else null

    override fun getValues(): List<Double>? = getBucketsInTimeWindow()?.flatMap { bucket ->
        bucket.histogram.recordedValues().flatMap { bin ->
            List(bin.countAtValueIteratedTo.toInt()) {
                bin.valueIteratedTo.toDouble()
            }
        }
    }

    /**
     * @return the maximum value registered in the time window. Note: differently from the values returned by
     * [getValues], this is NOT approximated.
     */
    fun max(): Double? = getBucketsInTimeWindow()?.maxOfOrNull { it.max }?.toDouble()

    /**
     * @return the sum of the values registered in the time window. Note: differently from the values returned by
     *      * [getValues], this is NOT approximated.
     */
    fun sum(): Double? = getBucketsInTimeWindow()?.sumOf { it.sum }?.toDouble()

    /**
     * @return the average value registered in the time window. Note: differently from the values returned by
     * [getValues], this is NOT approximated.
     * Since each bucket in the window already computes an average, the average of the average is taken. This converges
     * to the statistical average as per [LLN](https://en.wikipedia.org/wiki/Law_of_large_numbers).
     */
    fun average(): Double? = getBucketsInTimeWindow()?.mapNotNull {
        it.histogram.totalCount.takeIf { count ->
            count > 0
        }?.let { count ->
            it.sum / count
        }
    }?.takeIf {
        it.isNotEmpty()
    }?.average()
}
