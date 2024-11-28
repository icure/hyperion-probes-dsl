package com.icure.monitoring.meters

import io.micrometer.core.instrument.distribution.CountAtBucket
import io.micrometer.core.instrument.distribution.HistogramSnapshot
import io.micrometer.core.instrument.distribution.ValueAtPercentile
import org.HdrHistogram.Histogram
import org.HdrHistogram.Recorder
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

data class HistogramBucket(
	private val recorder: Recorder,
	private val sumAccumulator: AtomicLong = AtomicLong(0),
	private val maxAccumulator: AtomicLong = AtomicLong(0),
) {
	companion object {
		fun create(lowestValue: Long, highestValue: Long, significantDigits: Int) =
			HistogramBucket(Recorder(lowestValue, highestValue, significantDigits))
	}
	val sum: Long get() = sumAccumulator.get()
	val max: Long get() = maxAccumulator.get()
	private var recorderSnapshot: Histogram? = null
	val histogram: Histogram get() = recorderSnapshot ?: recorder.intervalHistogram.also {
		recorderSnapshot = it
	}

	fun addValue(amount: Double) {
		recorder.recordValue(amount.toLong())
		sumAccumulator.addAndGet(amount.toLong())
		maxAccumulator.accumulateAndGet(amount.toLong()) { current, new -> max(current, new) }
	}

	fun toHistogramSnapshot(percentiles: Iterable<Double>? = null, summaryOutput: ((PrintStream, Double) -> Unit)? = null): HistogramSnapshot =
		histogram.let { histogram ->
			HistogramSnapshot(
				histogram.totalCount,
				sum.toDouble(),
				max.toDouble(),
				percentiles?.map {
					// Micrometer wants the percentiles expressed between 0 and 1, HdrHistogram wants them between 0 and 100
					ValueAtPercentile(it / 100, histogram.getValueAtPercentile(it).toDouble())
				}?.toTypedArray(),
				histogram.recordedValues().map {
					CountAtBucket(it.valueIteratedTo.toDouble(), it.countAtValueIteratedTo.toDouble())
				}.toTypedArray(),
				summaryOutput
			)
		}

}