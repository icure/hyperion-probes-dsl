package com.icure.monitoring.meters

import com.dynatrace.dynahist.Histogram
import com.dynatrace.dynahist.layout.Layout
import io.micrometer.core.instrument.distribution.CountAtBucket
import io.micrometer.core.instrument.distribution.HistogramSnapshot
import io.micrometer.core.instrument.distribution.ValueAtPercentile
import java.io.PrintStream
import kotlin.math.max

data class HistogramBucket(
	val histogram: Histogram,
	private var sumAccumulator: Double = 0.0,
	private var maxAccumulator: Double = 0.0
) {
	companion object {
		fun create(layout: Layout) = HistogramBucket(Histogram.createDynamic(layout))
	}
	val sum: Double get() = sumAccumulator
	val max: Double get() = maxAccumulator

	fun addValue(amount: Double) {
		histogram.addValue(amount)
		sumAccumulator += amount
		maxAccumulator = max(maxAccumulator, amount)
	}

	fun toHistogramSnapshot(percentiles: Iterable<Double>? = null, summaryOutput: ((PrintStream, Double) -> Unit)? = null): HistogramSnapshot =
		histogram.preprocessedCopy.let { immutableHistogram ->
			HistogramSnapshot(
				immutableHistogram.totalCount,
				sum,
				max,
				percentiles?.map { ValueAtPercentile(it, immutableHistogram.getQuantile(it)) }?.toTypedArray(),
				immutableHistogram.nonEmptyBinsAscending().map { CountAtBucket(it.binIndex.toDouble(), it.binCount.toDouble()) }.toTypedArray(),
				summaryOutput
			)
		}

}