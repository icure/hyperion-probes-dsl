package com.icure.monitoring.test.fake

import com.icure.monitoring.test.uuid
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.distribution.HistogramSnapshot
import io.micrometer.core.instrument.distribution.ValueAtPercentile

/**
 * A [DistributionSummary] whose snapshot exposes pre-set percentile values, like the BucketDistributionSummary
 * registered with `publishPercentiles` does.
 *
 * @param percentiles a map from the percentile (between 0 and 1) to its value.
 */
class PercentileDistributionSummary(
    private val name: String = uuid(),
    private val tags: List<Tag> = emptyList(),
    private val percentiles: Map<Double, Double>
) : DistributionSummary {

    override fun getId(): Meter.Id =
        Meter.Id(name, Tags.empty().and(*tags.toTypedArray()), null, null, Meter.Type.DISTRIBUTION_SUMMARY)

    override fun takeSnapshot(): HistogramSnapshot =
        HistogramSnapshot(
            1,
            percentiles.values.sum(),
            percentiles.values.maxOrNull() ?: 0.0,
            percentiles.map { (percentile, value) -> ValueAtPercentile(percentile, value) }.toTypedArray(),
            null,
            null
        )

    override fun record(amount: Double) {}

    override fun count(): Long = 1

    override fun totalAmount(): Double = percentiles.values.sum()

    override fun max(): Double = percentiles.values.maxOrNull() ?: 0.0
}
