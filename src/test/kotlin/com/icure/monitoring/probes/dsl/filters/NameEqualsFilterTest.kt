package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.test.generateMeter
import com.icure.monitoring.test.shouldMatch
import com.icure.monitoring.test.shouldNotMatch
import com.icure.monitoring.test.uuid
import io.kotest.core.spec.style.StringSpec
import io.micrometer.core.instrument.Tag

class NameEqualsFilterTest : StringSpec({

    "A NameEqualsFilter should match only the meters that match the condition" {
        val name = uuid()
        val filter = NameEqualsFilter(name)

        filter shouldMatch generateMeter(name)
        filter shouldNotMatch generateMeter(uuid())
    }

    "A NameEqualsFilter can be combined with other filters through and" {
        val name = uuid()
        val tagValue = uuid()
        val compositeFilter = metricNameIs(name) and TagEqualsFilter(MetricsTags.METRIC, tagValue)

        compositeFilter shouldMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldNotMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldNotMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldNotMatch generateMeter()
    }

    "A NameEqualsFilter can be combined with other filters through or" {
        val name = uuid()
        val tagValue = uuid()
        val compositeFilter = metricNameIs(name) or TagEqualsFilter(MetricsTags.METRIC, tagValue)

        compositeFilter shouldMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldNotMatch generateMeter()
    }

})
