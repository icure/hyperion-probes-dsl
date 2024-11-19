package com.icure.monitoring.probes.dsl.collectors

import com.icure.monitoring.test.fake.FakeClock
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Duration

class TimeWindowCollectorTest : StringSpec({

    "When using a TimeWindowCollector, only the samples in the window are considered" {
        val clock = FakeClock()
        var now = clock.wallTime()
        val collector = TimeWindowCollector(Duration.ofMinutes(1), Duration.ofSeconds(1), clock)
        collector.addValue(42.0)

        val size = 60
        val commonValue = 1.0
        val offset = 1_000L

        (0 .. size).forEach { _ ->
            clock.advance(offset)
            now += offset
            collector.addValue(commonValue)
        }
        collector.getValues() shouldBe (0 .. size).map { commonValue }
        collector.max() shouldBe 1.0
    }

    "A TimeWindowCollector will aggregate the samples with the specified granularity" {
        val clock = FakeClock()
        val timeFrame = Duration.ofMinutes(1)
        val collector = TimeWindowCollector(timeFrame, Duration.ofSeconds(1), clock)

        // I advance the clock of one timeframe,
        // otherwise the collector will return null because less than a timeframe passed
        clock.advance(timeFrame.toMillis())
        val size = 10
        val samples = 100
        (0 until size).forEach { value ->
            clock.advance(1_000)
            (0 until samples).forEach { _ ->
                collector.addValue(value.toDouble())
            }
        }


        collector.getValues() shouldBe (0 until size).flatMap { v -> (0 until samples).map { v.toDouble() } }
        collector.max() shouldBe (size - 1).toDouble()
    }

})