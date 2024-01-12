package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.test.generateMeter
import com.icure.monitoring.test.shouldMatch
import com.icure.monitoring.test.shouldNotMatch
import com.icure.monitoring.test.uuid
import io.kotest.core.spec.style.StringSpec
import io.micrometer.core.instrument.Tag

class MatchTagFilterTest : StringSpec({

    "A MatchTagFilter should match only the meters that match the condition" {
        val tagType = MetricsTags.METRIC
        val tagValue = uuid()
        val filter = TagEqualsFilter(tagType, tagValue)

        filter shouldMatch generateMeter(tags = listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        filter shouldNotMatch generateMeter()
    }

    "A MatchTagFilter can be combined with other filters through and" {
        val name = uuid()
        val tagType = MetricsTags.METRIC
        val tagValue = "^[0-9]{5}$"
        val compositeFilter = metricNameIs(name) and (tagType isEqualTo tagValue)

        compositeFilter shouldMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, "12345")))
        compositeFilter shouldNotMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, "12345")))
        compositeFilter shouldNotMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldNotMatch generateMeter()
    }

    "A MatchTagFilter can be combined with other filters through or" {
        val name = uuid()
        val tagType = MetricsTags.METRIC
        val tagValue = "^[0-9]{5}$"
        val compositeFilter = metricNameIs(name) or (tagType isEqualTo tagValue)

        compositeFilter shouldMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, "12345")))
        compositeFilter shouldMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, "12345")))
        compositeFilter shouldMatch generateMeter(name, listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldNotMatch generateMeter()
    }

})
