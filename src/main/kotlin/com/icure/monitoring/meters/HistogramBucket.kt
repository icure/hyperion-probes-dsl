package com.icure.monitoring.meters

import com.google.common.util.concurrent.AtomicDouble
import io.micrometer.core.instrument.distribution.CountAtBucket
import io.micrometer.core.instrument.distribution.HistogramSnapshot
import io.micrometer.core.instrument.distribution.ValueAtPercentile
import org.HdrHistogram.Histogram
import org.HdrHistogram.Recorder
import java.io.PrintStream
import kotlin.math.max

data class HistogramBucket(
	private val recorder: Recorder,
	private val sumAccumulator: AtomicDouble = AtomicDouble(0.0),
	private val maxAccumulator: AtomicDouble = AtomicDouble(0.0),
) {
	companion object {
		fun create(lowestValue: Long, highestValue: Long, significantDigits: Int) =
			HistogramBucket(Recorder(lowestValue, highestValue, significantDigits))
	}
	val sum: Double get() = sumAccumulator.get()
	val max: Double get() = maxAccumulator.get()
	private var recorderSnapshot: Histogram? = null
	val histogram: Histogram get() = recorderSnapshot ?: recorder.intervalHistogram.also {
		recorderSnapshot = it
	}

	fun addValue(amount: Double) {
		recorder.recordValue(amount.toLong())
		sumAccumulator.addAndGet(amount)
		maxAccumulator.accumulateAndGet(amount) { current, new -> max(current, new) }
	}

	fun toHistogramSnapshot(percentiles: Iterable<Double>? = null, summaryOutput: ((PrintStream, Double) -> Unit)? = null): HistogramSnapshot =
		histogram.let { histogram ->
			HistogramSnapshot(
				histogram.totalCount,
				sum,
				max,
				percentiles?.map {
					// Micrometer wants the percentiles expressed between 0 and 1, HdrHistogram wants them between 0 and 100
					ValueAtPercentile(it / 100, histogram.getValueAtPercentile(it).toDouble())
				}?.toTypedArray(),
				histogram.recordedValues().map {
					CountAtBucket(it.doubleValueIteratedTo, it.countAtValueIteratedTo.toDouble())
				}.toTypedArray(),
				summaryOutput
			)
		}

}
