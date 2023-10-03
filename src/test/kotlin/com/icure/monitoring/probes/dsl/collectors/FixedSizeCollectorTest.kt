package com.icure.monitoring.probes.dsl.collectors

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class FixedSizeCollectorTest : StringSpec({

    "A fixed size collector should never exceed the window size" {
        val size = 10
        val collector = FixedSizeCollector(size)
        (1 .. 1000).map { _ ->
            async {
                (1 .. 10000).forEach {
                    collector.addValue(it.toDouble())
                }
            }
        }.awaitAll()

        collector.getValues().size shouldBe size
    }

    "A fixed size collector can retrieve values multiple times" {
        val size = 10
        val collector = FixedSizeCollector(size)
        (1 .. 1000).forEach {
            collector.addValue(it.toDouble())
        }
        collector.getValues() shouldBe collector.getValues()
        collector.getValues().size shouldBe 10
    }

})