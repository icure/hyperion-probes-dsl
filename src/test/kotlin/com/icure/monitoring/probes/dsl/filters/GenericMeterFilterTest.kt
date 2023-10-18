package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.test.generateMeter
import com.icure.monitoring.test.shouldMatch
import com.icure.monitoring.test.shouldNotMatch
import com.icure.monitoring.test.uuid
import io.kotest.core.spec.style.StringSpec
import io.micrometer.core.instrument.Tag

class GenericMeterFilterTest : StringSpec({

    "A GenericMeterFilter should match only the meters that match the condition" {
        val filter = GenericMeterFilter { meter ->
            meter.id.tags.any { it.value == "tag" }
        }

        filter shouldMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.COUCHDB_TASK_STATUS.tagName, "tag")))
        filter shouldNotMatch generateMeter(uuid())
    }

    "A GenericMeterFilter can be combined with other filters through and" {
        val filter = GenericMeterFilter {
            !it.id.name.startsWith("prefix")
        }
        val tagValue = uuid()
        val compositeFilter = filter and MatchTagFilter(MetricsTags.METRIC, tagValue)

        compositeFilter shouldMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldNotMatch generateMeter("prefix-${uuid()}-suffix", listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldNotMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldNotMatch generateMeter()
    }

    "A GenericMeterFilter can be combined with other filters through or" {
        val filter = GenericMeterFilter {
            !it.id.name.endsWith("suffix")
        }
        val tagValue = uuid()
        val compositeFilter = filter or MatchTagFilter(MetricsTags.METRIC, tagValue)

        compositeFilter shouldMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldMatch generateMeter("${uuid()}-suffix", listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        compositeFilter shouldMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        compositeFilter shouldNotMatch generateMeter("${uuid()}-suffix")
    }

})