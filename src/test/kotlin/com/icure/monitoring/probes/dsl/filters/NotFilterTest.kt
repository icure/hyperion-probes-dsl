package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.test.generateMeter
import com.icure.monitoring.test.shouldMatch
import com.icure.monitoring.test.shouldNotMatch
import com.icure.monitoring.test.uuid
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Tag

class NotFilterTest : StringSpec({

    "A NotFilter should match only the meters that do not match the inner filter" {
        val tagValue = uuid()
        val notFilter = not(TagEqualsFilter(MetricsTags.URL, tagValue))

        notFilter shouldMatch generateMeter(tags = listOf(Tag.of(MetricsTags.URL.tagName, uuid())))
        notFilter shouldMatch generateMeter()
        notFilter shouldNotMatch generateMeter(tags = listOf(Tag.of(MetricsTags.URL.tagName, tagValue)))
    }

    "A NotFilter negates a composite inner filter" {
        val urlA = uuid()
        val urlB = uuid()
        val notFilter = not(TagEqualsFilter(MetricsTags.URL, urlA) or TagEqualsFilter(MetricsTags.URL, urlB))

        notFilter shouldNotMatch generateMeter(tags = listOf(Tag.of(MetricsTags.URL.tagName, urlA)))
        notFilter shouldNotMatch generateMeter(tags = listOf(Tag.of(MetricsTags.URL.tagName, urlB)))
        notFilter shouldMatch generateMeter(tags = listOf(Tag.of(MetricsTags.URL.tagName, uuid())))
    }

    "A NotFilter resolves to a must_not boolean ES query" {
        not(TagEqualsFilter(MetricsTags.URL, "https://foo/status")).toElasticQuery() shouldBe
            """"bool":{"must_not":[{"term":{"url":"https://foo/status"}}]}"""
    }

    "A NotFilter can be combined with other filters through and" {
        val name = uuid()
        val url = uuid()
        val filter = NameEqualsFilter(name) and not(TagEqualsFilter(MetricsTags.URL, url))

        filter shouldMatch generateMeter(name, listOf(Tag.of(MetricsTags.URL.tagName, uuid())))
        filter shouldNotMatch generateMeter(name, listOf(Tag.of(MetricsTags.URL.tagName, url)))
        filter shouldNotMatch generateMeter(uuid(), listOf(Tag.of(MetricsTags.URL.tagName, uuid())))
    }

})
