package com.icure.monitoring.probes.dsl.threshold

import com.icure.monitoring.probes.dsl.comparators.ThresholdValue

/**
 * A fixed threshold value.
 *
 * @param thresholdValue the value.
 */
class FixedValueThreshold(
    private val thresholdValue: ThresholdValue
) : Threshold {
    override fun getValue() = thresholdValue
}