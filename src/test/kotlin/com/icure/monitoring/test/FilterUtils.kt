package com.icure.monitoring.test

import com.icure.monitoring.probes.dsl.filters.Filter
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Meter

infix fun Filter.shouldMatch(meter: Meter) {
    this.matches(meter) shouldBe true
}

infix fun Filter.shouldNotMatch(meter: Meter) {
    this.matches(meter) shouldBe false
}