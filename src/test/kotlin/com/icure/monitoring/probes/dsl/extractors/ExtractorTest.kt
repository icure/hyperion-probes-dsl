package com.icure.monitoring.probes.dsl.extractors

import com.icure.monitoring.test.fake.FakeDistributionSummary
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.internal.DefaultGauge
import java.util.*
import kotlin.random.Random

class ExtractorTest : StringSpec({

    "Is it possible to define a custom extractor" {
        val gaugeValue = Random.nextDouble(0.0, 42.0)
        val gauge = DefaultGauge(
            Meter.Id(
                UUID.randomUUID().toString(), Tags.empty(), null, null, Meter.Type.GAUGE),
            gaugeValue
        ) { gaugeValue }
        val ds = FakeDistributionSummary()


        val customExtractor = extractor { m ->
            if(m is Gauge) m.value().takeIf { it.isFinite() }?.times(2)
            else null
        }

        customExtractor.valueOf(gauge) shouldBe gaugeValue*2
        customExtractor.valueOf(ds).shouldBeNull()
    }

})