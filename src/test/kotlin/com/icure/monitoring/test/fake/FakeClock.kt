package com.icure.monitoring.test.fake

import io.micrometer.core.instrument.Clock

class FakeClock(
    startingValue: Long? = null
) : Clock {

    private var value = startingValue ?: System.currentTimeMillis()

    fun advance(offset: Long) {
        value += offset
    }

    override fun wallTime(): Long = value

    override fun monotonicTime(): Long = value

}