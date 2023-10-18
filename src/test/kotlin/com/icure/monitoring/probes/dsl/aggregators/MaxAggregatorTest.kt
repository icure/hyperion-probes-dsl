package com.icure.monitoring.probes.dsl.aggregators

import com.icure.monitoring.probes.dsl.collectors.FixedSizeCollector
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class MaxAggregatorTest : StringSpec({

    "A MaxAggregator can get the max of the values stored in a collector" {
        val size = 10
        val collector = FixedSizeCollector(size)
        val values = List(size) { Random.nextDouble(0.0, 42.0) }.onEach {
            collector.addValue(it)
        }
        MaxAggregator.aggregate(collector) shouldBe values.max()
    }


})