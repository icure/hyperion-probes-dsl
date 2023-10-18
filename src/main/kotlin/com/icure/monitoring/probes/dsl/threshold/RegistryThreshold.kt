package com.icure.monitoring.probes.dsl.threshold

import com.icure.monitoring.probes.dsl.DataAggregationChain
import com.icure.monitoring.probes.dsl.comparators.ThresholdValue
import com.icure.monitoring.probes.dsl.data.RegistryDataSource
import io.micrometer.core.instrument.Meter

/**
 * a [RegistryThreshold] runs a parallel [DataAggregationChain] w.r.t. the one of the probe. It will filter,
 * extract and aggregate data using different criteria, and then it will provide a threshold that depends on the
 * collected values.
 *
 * @param config a [DataAggregationChain] that defines the parameters of the threshold.
 */
class RegistryThreshold(
    config: DataAggregationChain
) : Threshold {

    private val registryId: String = config.definedDataSource.takeIf { it is RegistryDataSource }?.let {
        (it as RegistryDataSource).registryId
    } ?: throw IllegalArgumentException("Only RegistryDataSource can be used in threshold")

    private val extractor = config.definedExtractor
    private val filter = config.definedFilter
    private val collector = config.collectorProducer()
    private val aggregator = config.definedAggregator

    /**
     * Receives a [Meter] from a registry and stores its value.
     *
     * @param meter the [Meter] to process.
     * @param submittingRegistryId the id of the registry that sent the meter.
     */
    suspend fun receiveMeter(meter: Meter, submittingRegistryId: String) {
        val extractedValue = extractor.valueOf(meter)
        if(extractedValue != null && registryId == submittingRegistryId && filter.matches(meter)) {
            collector.addValue(extractedValue)
        }
    }

    override fun getValue(): ThresholdValue? = aggregator.aggregate(collector)

}