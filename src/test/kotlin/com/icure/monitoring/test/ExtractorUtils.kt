package com.icure.monitoring.test

import com.icure.monitoring.probes.dsl.collectors.TimeWindowCollector
import com.icure.monitoring.probes.dsl.extractors.ExtractorFactory
import com.icure.monitoring.probes.dsl.extractors.SingleExtractorFactory
import com.icure.monitoring.probes.dsl.utils.ExtractorFactoryParams
import com.icure.monitoring.probes.dsl.utils.ExtractorParams
import com.icure.monitoring.probes.dsl.utils.over
import io.micrometer.core.instrument.Clock
import java.time.Duration

fun ExtractorFactory.overWithFakeClock(duration: Duration, clock: Clock) = ExtractorFactoryParams(this) {
    TimeWindowCollector(duration, Duration.ofSeconds(60), clock)
}

fun SingleExtractorFactory.overWithFakeClock(duration: Duration, clock: Clock) = ExtractorParams(this.getExtractor()) {
    TimeWindowCollector(duration, Duration.ofSeconds(60), clock)
}