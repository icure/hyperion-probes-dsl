package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.test.generateMeter
import com.icure.monitoring.test.shouldMatch
import com.icure.monitoring.test.shouldNotMatch
import io.kotest.core.spec.style.StringSpec

class MetricValueBetweenFilterTest : StringSpec({

    "A MetricValueBetweenFilter should match only the meters that match the condition" {
        val filter = gaugeValueBetween(1.0, 2.0)

        filter shouldMatch generateMeter(value = 1.5)
        filter shouldNotMatch generateMeter(value = 0.0)
    }

    "A MetricValueBetweenFilter can be combined with other filters through and" {
        val name = "abc"
        val compositeFilter = gaugeValueBetween(1.0, 2.0) and metricNameIs(name)

        compositeFilter shouldMatch generateMeter(name = name, value = 1.5)
        compositeFilter shouldNotMatch generateMeter(value = 1.5)
        compositeFilter shouldNotMatch generateMeter(name = name, value = 0.0)
        compositeFilter shouldNotMatch generateMeter(value = 0.0)
    }

    "A MetricValueBetweenFilter can be combined with other filters through or" {
        val name = "def"
        val compositeFilter = gaugeValueBetween(1.0, 2.0) or metricNameIs(name)

        compositeFilter shouldMatch generateMeter(name = name, value = 1.5)
        compositeFilter shouldMatch generateMeter(value = 1.5)
        compositeFilter shouldMatch generateMeter(name = name, value = 0.0)
        compositeFilter shouldNotMatch generateMeter(value = 0.0)
    }

})
