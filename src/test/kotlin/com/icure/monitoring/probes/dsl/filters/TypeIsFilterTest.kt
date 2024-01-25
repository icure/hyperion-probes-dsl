package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.test.generateMeter
import com.icure.monitoring.test.shouldMatch
import com.icure.monitoring.test.shouldNotMatch
import com.icure.monitoring.test.uuid
import io.kotest.core.spec.style.StringSpec
import io.micrometer.core.instrument.Meter

class TypeIsFilterTest : StringSpec({

    "A TypeIsFilter should match only the meters that match the condition" {
        val meterType = Meter.Type.COUNTER
        val filter = TypeIsFilter(meterType)

        filter shouldMatch generateMeter(type = meterType)
        filter shouldNotMatch generateMeter()
    }

    "A TypeIsFilter can be combined with other filters through and" {
        val name = uuid()
        val compositeFilter = metricNameIs(name) and meterIsAGauge()

        compositeFilter shouldMatch generateMeter(name, type = Meter.Type.GAUGE)
        compositeFilter shouldNotMatch generateMeter(uuid(), type = Meter.Type.GAUGE)
        compositeFilter shouldNotMatch generateMeter(name, type = Meter.Type.DISTRIBUTION_SUMMARY)
    }

    "A TypeIsFilter can be combined with other filters through or" {
        val name = uuid()
        val compositeFilter = metricNameIs(name) or meterIsADistribution()

        compositeFilter shouldMatch generateMeter(name, type = Meter.Type.DISTRIBUTION_SUMMARY)
        compositeFilter shouldMatch generateMeter(uuid(), type = Meter.Type.DISTRIBUTION_SUMMARY)
        compositeFilter shouldMatch generateMeter(name)
        compositeFilter shouldNotMatch generateMeter()
    }

})
