package com.icure.monitoring.probes

import com.icure.monitoring.meters.HistogramBucket
import com.icure.monitoring.probes.dsl.ProbeConfig
import java.time.Duration

/**
 * Concrete probe that can be attached to a micrometer registry.
 */
class RegistryProbe(
    val registryId: String,
    val samplingInterval: Duration,
    config: ProbeConfig
) : Probe(config) {

    val windowInterval: Duration = trigger.timeFrame

    fun calculateCurrentLevel(data: Collection<HistogramBucket>): Double? = trigger.calculateCurrentLevel(data)

}