package com.icure.monitoring.utils

import com.icure.monitoring.probes.Probe

interface MultipleProbesGenerator {

    fun generateMultiple(): Map<String, Probe>

}