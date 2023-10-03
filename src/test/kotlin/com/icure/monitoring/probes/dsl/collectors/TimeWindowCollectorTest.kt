package com.icure.monitoring.probes.dsl.collectors

import com.icure.monitoring.test.FakeClock
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Duration

class TimeWindowCollectorTest : StringSpec({

    "When using a TimeWindowCollector, only the samples in the window are considered" {
        val clock = FakeClock()
        val collector = TimeWindowCollector(Duration.ofMinutes(1), Duration.ofSeconds(1), clock)
        collector.addValue(42.0)

        val size = 60
        val commonValue = 1.0

        (0 until size).forEach { _ ->
            clock.advance(1_000)
            collector.addValue(commonValue)
        }
        collector.getValues() shouldBe List(size) { commonValue }
        collector.max() shouldBe 1.0
    }

    "A TimeWindowCollector will aggregate the samples with the specified granularity" {
        val clock = FakeClock()
        val collector = TimeWindowCollector(Duration.ofMinutes(1), Duration.ofSeconds(1), clock)

        val size = 10
        (0 until size).forEach { value ->
            clock.advance(1_000)
            (0 .. 1000).forEach { _ ->
                collector.addValue(value.toDouble())
            }
        }

        collector.getValues() shouldBe (0 until size).map { it.toDouble() }
        collector.max() shouldBe (size - 1).toDouble()
    }

})