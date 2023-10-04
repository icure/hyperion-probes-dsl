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
        val aggregator = aggregator {
            it.getValues().sum() / 10
        }

        aggregator.aggregate(collector) shouldBe (values.sum() / 10)
    }

})