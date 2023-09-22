package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.test.generateMeter
import com.icure.monitoring.test.shouldMatch
import com.icure.monitoring.test.shouldNotMatch
import com.icure.monitoring.test.uuid
import io.kotest.core.spec.style.StringSpec
import io.micrometer.core.instrument.Tag

class AndFilterTest : StringSpec({

    "An AndFilter should match only the meters that satisfy all the conditions" {
        val filterName = uuid()
        val tagValue = uuid()
        val andFilter = MatchNameFilter(filterName) and MatchTagFilter(MetricsTags.METRIC, tagValue)

        andFilter shouldMatch generateMeter(filterName, listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        andFilter shouldNotMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.METRIC.tagName, tagValue)))
        andFilter shouldNotMatch generateMeter(filterName, listOf(Tag.of(MetricsTags.METRIC.tagName, uuid())))
        andFilter shouldNotMatch generateMeter(filterName, listOf(Tag.of(MetricsTags.BACKEND.tagName, tagValue)))
        andFilter shouldNotMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.BACKEND.tagName, tagValue)))
        andFilter shouldNotMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.BACKEND.tagName, uuid())))
        andFilter shouldNotMatch generateMeter(uuid())
    }

    "AndFilters can be combined with and" {
        val filterName = uuid()
        val tagValue1 = uuid()
        val filter1 = MatchNameFilter(filterName) and MatchTagFilter(MetricsTags.METRIC, tagValue1)

        val tagValue2 = uuid()
        val tagValue3 = uuid()
        val filter2 = MatchTagFilter(MetricsTags.BACKEND, tagValue2) and MatchTagFilter(MetricsTags.TYPE, tagValue3)

        val andFilter = filter1 and filter2
        andFilter shouldMatch generateMeter(
            filterName,
            listOf(
                Tag.of(MetricsTags.METRIC.tagName, tagValue1),
                Tag.of(MetricsTags.BACKEND.tagName, tagValue2),
                Tag.of(MetricsTags.TYPE.tagName, tagValue3),
            ))
        andFilter shouldNotMatch generateMeter(
            uuid(),
            listOf(
                Tag.of(MetricsTags.METRIC.tagName, tagValue1),
                Tag.of(MetricsTags.BACKEND.tagName, tagValue2),
                Tag.of(MetricsTags.TYPE.tagName, tagValue3),
            ))
        andFilter shouldNotMatch generateMeter(
            filterName,
            listOf(
                Tag.of(MetricsTags.METRIC.tagName, uuid()),
                Tag.of(MetricsTags.BACKEND.tagName, tagValue2),
                Tag.of(MetricsTags.TYPE.tagName, tagValue3)
            ))
        andFilter shouldNotMatch generateMeter(
            filterName,
            listOf(
                Tag.of(MetricsTags.METRIC.tagName, tagValue1),
                Tag.of(MetricsTags.BACKEND.tagName, uuid()),
                Tag.of(MetricsTags.TYPE.tagName, tagValue3),
            ))
        andFilter shouldNotMatch generateMeter(
            filterName,
            listOf(
                Tag.of(MetricsTags.METRIC.tagName, tagValue1),
                Tag.of(MetricsTags.BACKEND.tagName, tagValue2),
                Tag.of(MetricsTags.TYPE.tagName, uuid())
            ))
        andFilter shouldNotMatch generateMeter(filterName)
        andFilter shouldNotMatch generateMeter(
            tags = listOf(
                Tag.of(MetricsTags.METRIC.tagName, tagValue1),
                Tag.of(MetricsTags.BACKEND.tagName, tagValue2),
                Tag.of(MetricsTags.TYPE.tagName, tagValue3)
            ))
    }

    "AndFilters can be combined with or" {
        val filterName = uuid()
        val tagValue1 = uuid()
        val filter1 = MatchNameFilter(filterName) and MatchTagFilter(MetricsTags.METRIC, tagValue1)

        val tagValue2 = uuid()
        val tagValue3 = uuid()
        val filter2 = MatchTagFilter(MetricsTags.BACKEND, tagValue2) and MatchTagFilter(MetricsTags.TYPE, tagValue3)

        val andFilter = filter1 or filter2
        andFilter shouldMatch generateMeter(
            filterName,
            listOf(
                Tag.of(MetricsTags.METRIC.tagName, tagValue1),
                Tag.of(MetricsTags.BACKEND.tagName, tagValue2),
                Tag.of(MetricsTags.TYPE.tagName, tagValue3)
            ))
        andFilter shouldMatch generateMeter(
            uuid(),
            listOf(
                Tag.of(MetricsTags.METRIC.tagName, tagValue1),
                Tag.of(MetricsTags.BACKEND.tagName, tagValue2),
                Tag.of(MetricsTags.TYPE.tagName, tagValue3)
            ))
        andFilter shouldMatch generateMeter(
            filterName,
            listOf(
                Tag.of(MetricsTags.METRIC.tagName, uuid()),
                Tag.of(MetricsTags.BACKEND.tagName, tagValue2),
                Tag.of(MetricsTags.TYPE.tagName, tagValue3)
            ))
        andFilter shouldMatch generateMeter(
            filterName,
            listOf(
                Tag.of(MetricsTags.METRIC.tagName, tagValue1),
                Tag.of(MetricsTags.BACKEND.tagName, uuid()),
                Tag.of(MetricsTags.TYPE.tagName, tagValue3),
            ))
        andFilter shouldMatch generateMeter(
            filterName,
            listOf(
                Tag.of(MetricsTags.METRIC.tagName, tagValue1),
                Tag.of(MetricsTags.BACKEND.tagName, tagValue2),
                Tag.of(MetricsTags.TYPE.tagName, uuid()),
            ))
        andFilter shouldNotMatch generateMeter(filterName)
        andFilter shouldNotMatch generateMeter(
            tags = listOf(
                Tag.of(MetricsTags.METRIC.tagName, tagValue1),
                Tag.of(MetricsTags.BACKEND.tagName, tagValue2),
            ))
    }

})