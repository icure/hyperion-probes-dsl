package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.test.generateMeter
import com.icure.monitoring.test.shouldMatch
import com.icure.monitoring.test.shouldNotMatch
import com.icure.monitoring.test.uuid
import io.kotest.core.spec.style.StringSpec
import io.micrometer.core.instrument.Tag

class NoOpFilterTest : StringSpec({

    "A NoOpFilter should match all the filters" {
        NoOpFilter shouldMatch generateMeter(uuid())
        NoOpFilter shouldMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
    }

    "A NoOpFilter is the neutral element of an and chain" {
        val name = uuid()
        val compositeFilter = metricNameIs(name) and NoOpFilter

        compositeFilter shouldMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldNotMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldNotMatch generateMeter()
    }

    "A NoOpFilter is the null element of an or chain" {
        val name = uuid()
        val compositeFilter = metricNameIs(name) or NoOpFilter

        compositeFilter shouldMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldMatch generateMeter()
    }

})