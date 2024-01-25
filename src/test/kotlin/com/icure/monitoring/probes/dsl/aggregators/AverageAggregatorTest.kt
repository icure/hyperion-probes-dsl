package com.icure.monitoring.probes.dsl.aggregators

import com.icure.monitoring.probes.dsl.collectors.FixedSizeCollector
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class AverageAggregatorTest : StringSpec({

    "An AverageAggregator can get the average of the values stored in a collector" {
        val size = 10
        val collector = FixedSizeCollector(size)
        val values = List(size) { Random.nextDouble(0.0, 42.0) }.onEach {
            collector.addValue(it)
        }
        val nDecimals = 14
        val round = { double: Double -> String.format("%.${nDecimals}f", double) }
        AverageAggregator.aggregate(collector)?.let { round(it) } shouldBe round(values.average())
    }

})
