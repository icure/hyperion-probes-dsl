package com.icure.monitoring.test

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.internal.DefaultGauge
import java.util.*

fun generateMeter(
    name: String = uuid(),
    tags: List<Tag> = emptyList(),
    type: Meter.Type = Meter.Type.GAUGE,
    value: Double = 0.0
) = DefaultGauge(
    Meter.Id(
        name, Tags.empty().and(*tags.toTypedArray()), null, null, type
    ),
    value
) { value }

fun generateGauge(
    name: String = uuid(),
    tags: List<Tag> = emptyList(),
    value: Double = 0.0
) = DefaultGauge(
    Meter.Id(
        name, Tags.empty().and(*tags.toTypedArray()), null, null, Meter.Type.GAUGE
    ),
    value
) { value }


fun uuid() = UUID.randomUUID().toString()