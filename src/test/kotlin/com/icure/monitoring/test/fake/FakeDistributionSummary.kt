package com.icure.monitoring.test.fake

import com.icure.monitoring.test.uuid
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.distribution.HistogramSnapshot

class FakeDistributionSummary : DistributionSummary {

    private val records = mutableListOf<Double>()

    override fun getId(): Meter.Id =
        Meter.Id(uuid(), Tags.of("a", "b"), null, null, Meter.Type.DISTRIBUTION_SUMMARY)

    override fun takeSnapshot(): HistogramSnapshot =
        HistogramSnapshot(records.size.toLong(), records.sum(), records.max(), null, null, null)

    override fun record(amount: Double) {
        records.add(amount)
    }

    override fun count(): Long = records.size.toLong()

    override fun totalAmount(): Double = records.sum()

    override fun max(): Double = records.max()
}