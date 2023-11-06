package com.icure.monitoring.test.fake

import com.icure.monitoring.test.uuid
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.distribution.HistogramSnapshot

class PreSetDistributionSummary(
    private val name: String = uuid(),
    private val tags: List<Tag> = emptyList(),
    private val count: Long,
    private val sum: Double,
    private val max: Double
) : DistributionSummary {

    override fun getId(): Meter.Id =
        Meter.Id(name, Tags.empty().and(*tags.toTypedArray()), null, null, Meter.Type.DISTRIBUTION_SUMMARY)

    override fun takeSnapshot(): HistogramSnapshot =
        HistogramSnapshot(count, sum, max, null, null, null)

    override fun record(amount: Double) {}

    override fun count(): Long = count

    override fun totalAmount(): Double = sum

    override fun max(): Double = max
}