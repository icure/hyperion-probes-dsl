package com.icure.monitoring.probes.dsl.extractors

import com.icure.monitoring.test.fake.FakeDistributionSummary
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.internal.DefaultGauge
import java.util.*
import kotlin.random.Random

class DistributionSummaryExtractorTest : StringSpec({

    "A DistributionSummaryExtractor can get the max from a distribution summary" {
        val ds = FakeDistributionSummary()
        val values = List(10) { Random.nextDouble(0.0, 42.0) }.onEach {
            ds.record(it)
        }
        val maxOfDs = DistributionSummaryExtractor.forMaxAggregator()
        maxOfDs.value(ds) shouldBe values.max()
    }

    "A DistributionSummaryExtractor can get the average from a distribution summary" {
        val ds = FakeDistributionSummary()
        val values = List(10) { Random.nextDouble(0.0, 42.0) }.onEach {
            ds.record(it)
        }
        val averageOfDs = DistributionSummaryExtractor.forAverageAggregator()
        averageOfDs.value(ds) shouldBe values.average()
    }

    "A DistributionSummaryExtractor can get the count from a distribution summary" {
        val ds = FakeDistributionSummary()
        val values = List(10) { Random.nextDouble(0.0, 42.0) }.onEach {
            ds.record(it)
        }
        val countOfDs = DistributionSummaryExtractor.forCountAggregator()
        countOfDs.value(ds) shouldBe values.size
    }

    "All DistributionSummaryExtractors return null if the meter passed as parameter is not a distribution summary" {
        val gaugeValue = Random.nextDouble(0.0, 42.0)
        val gauge = DefaultGauge(
            Meter.Id(
                UUID.randomUUID().toString(), Tags.empty(), null, null, Meter.Type.GAUGE),
            gaugeValue
        ) { gaugeValue }

        val maxOfDs = DistributionSummaryExtractor.forMaxAggregator()
        val averageOfDs = DistributionSummaryExtractor.forAverageAggregator()
        val countOfDs = DistributionSummaryExtractor.forCountAggregator()
        maxOfDs.value(gauge).shouldBeNull()
        averageOfDs.value(gauge).shouldBeNull()
        countOfDs.value(gauge).shouldBeNull()
    }

})