package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.test.generateMeter
import com.icure.monitoring.test.shouldMatch
import com.icure.monitoring.test.shouldNotMatch
import com.icure.monitoring.test.uuid
import io.kotest.core.spec.style.StringSpec
import io.micrometer.core.instrument.Tag

class NameRegexFilterTest : StringSpec({

    "A NameRegexFilter should match only the meters that match the condition" {
        val pattern = "^[a-z]{3,5}$"
        val filter = NameRegexFilter(pattern)

        filter shouldMatch generateMeter("match")
        filter shouldNotMatch generateMeter(uuid())
    }

    "A NameRegexFilter can be combined with other filters through and" {
        val pattern = "^[a-z]{3,5}$"
        val tagValue = uuid()
        val compositeFilter = metricNameMatches(pattern) and TagEqualsFilter(MetricsTags.METRIC, tagValue)

        compositeFilter shouldMatch generateMeter("match", listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldNotMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldNotMatch generateMeter("match", listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldNotMatch generateMeter()
    }

    "A NameRegexFilter can be combined with other filters through or" {
        val pattern = "^[a-z]{3,5}$"
        val tagValue = uuid()
        val compositeFilter = metricNameMatches(pattern) or TagEqualsFilter(MetricsTags.METRIC, tagValue)

        compositeFilter shouldMatch generateMeter("match", listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldMatch generateMeter("match", listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldNotMatch generateMeter()
    }

})
