package com.icure.monitoring.probes.dsl.aggregators

import com.icure.monitoring.probes.dsl.collectors.FixedSizeCollector
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class AggregatorTest : StringSpec({

    "It is possible to define a custom aggregator" {
        val size = 10
        val collector = FixedSizeCollector(size)
        val values = List(size) { Random.nextDouble(0.0, 42.0) }.onEach {
            collector.addValue(it)
        }
        val nDecimals = 10
        val round = { double: Double -> String.format("%.${nDecimals}f", double) }
        val aggregator = aggregator {
            it.getValues()?.sum()?.div(10)
        }
        aggregator.aggregate(collector)?.let { round(it) } shouldBe round((values.sum() / 10))
    }

})
