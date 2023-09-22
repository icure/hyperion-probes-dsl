package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.test.generateMeter
import com.icure.monitoring.test.shouldMatch
import com.icure.monitoring.test.shouldNotMatch
import com.icure.monitoring.test.uuid
import io.kotest.core.spec.style.StringSpec
import io.micrometer.core.instrument.Tag

class MatchNameFilterTest : StringSpec({

    "A MatchNameFilter should match only the meters that match the condition" {
        val name = uuid()
        val filter = MatchNameFilter(name)

        filter shouldMatch generateMeter(name)
        filter shouldNotMatch generateMeter(uuid())
    }

    "A MatchNameFilter can be combined with other filters through and" {
        val name = uuid()
        val tagValue = uuid()
        val compositeFilter = metricNameIs(name) and MatchTagFilter(MetricsTags.METRIC, tagValue)

        compositeFilter shouldMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldNotMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldNotMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldNotMatch generateMeter()
    }

    "A MatchNameFilter can be combined with other filters through or" {
        val name = uuid()
        val tagValue = uuid()
        val compositeFilter = metricNameIs(name) or MatchTagFilter(MetricsTags.METRIC, tagValue)

        compositeFilter shouldMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldNotMatch generateMeter()
    }

})