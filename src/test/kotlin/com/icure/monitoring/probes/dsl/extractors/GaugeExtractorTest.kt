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

class GaugeExtractorTest : StringSpec({

    "A GaugeExtractor can get the max from a gauge" {
        val gaugeValue = Random.nextDouble(0.0, 42.0)
        val gauge = DefaultGauge(
            Meter.Id(
                UUID.randomUUID().toString(), Tags.empty(), null, null, Meter.Type.GAUGE),
            gaugeValue
        ) { gaugeValue }

        val maxOfGauge = GaugeExtractor.forMaxAggregator("value")
        maxOfGauge.valueOf(gauge) shouldBe gaugeValue
    }

    "A GaugeExtractor can get the average from a gauge" {
        val gaugeValue = Random.nextDouble(0.0, 42.0)
        val gauge = DefaultGauge(
            Meter.Id(
                UUID.randomUUID().toString(), Tags.empty(), null, null, Meter.Type.GAUGE),
            gaugeValue
        ) { gaugeValue }

        val averageOfGauge = GaugeExtractor.forAverageAggregator("value")
        averageOfGauge.valueOf(gauge) shouldBe gaugeValue
    }

    "A GaugeExtractor returns 1 when counting a gauge" {
        val gaugeValue = Random.nextDouble(0.0, 42.0)
        val gauge = DefaultGauge(
            Meter.Id(
                UUID.randomUUID().toString(), Tags.empty(), null, null, Meter.Type.GAUGE),
            gaugeValue
        ) { gaugeValue }

        val countOfGauge = GaugeExtractor.forCountAggregator("value")
        countOfGauge.valueOf(gauge) shouldBe 1.0
    }

    "All GaugeExtractors return null when a meter that is not a gauge is passed" {
        val ds = FakeDistributionSummary()

        val maxOfGauge = GaugeExtractor.forMaxAggregator("value")
        val averageOfGauge = GaugeExtractor.forAverageAggregator("value")
        val countOfGauge = GaugeExtractor.forCountAggregator("value")
        maxOfGauge.valueOf(ds).shouldBeNull()
        averageOfGauge.valueOf(ds).shouldBeNull()
        countOfGauge.valueOf(ds).shouldBeNull()
    }

})
