package com.icure.monitoring.utils

import com.icure.monitoring.probes.Probe

interface ProbeGenerator {
    fun generate(): Probe
}