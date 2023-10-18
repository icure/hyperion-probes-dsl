package com.icure.monitoring.probes.dsl.data

import com.icure.monitoring.probes.ElasticProbe
import com.icure.monitoring.probes.Probe
import com.icure.monitoring.probes.RegistryProbe
import com.icure.monitoring.probes.dsl.ProbeConfig

/**
 * Base class to configure a datasource for a probe.
 */
abstract class DataSource {

    companion object {
        /**
         * Defines a [RegistryDataSource] for the probe. A probe attached to a registry will receive data directly from
         * it and will dispatch its actions after the registry publishes the data, whether this succeeds or fails.
         * Only registry that are Identifiable can receive probes.
         */
        fun registry(block: RegistryDataSource.() -> Unit) = RegistryDataSource().apply(block)

        /**
         * Defines a [ElasticDataSource] for the probe. The probe will then fetch data periodically, as specified by the
         * cron in the configuration, and will dispatch its actions independently of the registries.
         */
        fun elastic(block: ElasticDataSource.() -> Unit) = ElasticDataSource().apply(block)
    }

    /**
     * Instantiates a concrete [Probe] based on the concrete data source class.
     */
    abstract fun createProbe(config: ProbeConfig): Probe

}

class RegistryDataSource : DataSource() {
    /**
     * The id of the registry to attach this probe to. Only Identifiable registries are supported.
     */
    lateinit var registryId: String

    override fun createProbe(config: ProbeConfig) = RegistryProbe(registryId, config)
}

class ElasticDataSource : DataSource() {
    /**
     * The elastic index to query.
     */
    lateinit var index: String

    /**
     * The cron configuration for the schedule.
     */
    var cron: String = "0 * * * * *"

    override fun createProbe(config: ProbeConfig) = ElasticProbe(index, cron, config)
}