@file:Suppress("UNCHECKED_CAST")

package com.icure.monitoring.probes.resources

import com.icure.monitoring.actions.Action
import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.actions.payload.JiraActionPayload
import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.probes.RegistryProbe
import com.icure.monitoring.probes.dsl.aggregators.aggregator
import com.icure.monitoring.probes.dsl.descriptors.byName
import com.icure.monitoring.probes.dsl.extractors.GaugeValue
import com.icure.monitoring.probes.dsl.filters.metricNameMatches
import com.icure.monitoring.probes.dsl.probe
import com.icure.monitoring.probes.dsl.utils.aggregateUsing
import com.icure.monitoring.probes.dsl.utils.lastProducedBy
import com.icure.monitoring.test.fake.FakeJiraAction
import com.icure.monitoring.test.generators.GaugeGenerator
import com.icure.monitoring.test.generators.VariableTag
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Tag
import java.time.Duration
import kotlin.random.Random

class HealthCheckProbeTest : StringSpec({

    "HealthCheckProbe test" {
        val registry = "minutelyLogsElasticProperties"
        val fakeJiraAction = FakeJiraAction()
        val probe = probe {
            probeId = "HealthChecks"
            dataSource {
                registry {
                    registryId = registry
                }
            }

            filter {
                metricNameMatches("http.service.health.check.*")
            }

            group {
                listOf(byName)
            }

            customAggregation {
                3 lastProducedBy GaugeValue aggregateUsing aggregator { collector ->
                    collector.getValues()?.takeIf { it.size > 2 }?.average()
                }
            }

            compare { value, referenceValue -> value < referenceValue }

            fixedThreshold { 1.0 }

            action {
                jira { _, _, descriptors ->
                    val name = descriptors.firstOrNull()?.v ?: "UNKNOWN"
                    JiraActionPayload(
                        ticketId = "HealthChecks-$name",
                        title = "$name is not available",
                        description = "Check services availability and certificate validity",
                        autoCloseAfter = Duration.ofHours(2).toMillis()
                    )
                }
            }
        } as RegistryProbe

        val triggerGenerator = GaugeGenerator(
            { "http.service.health.check.444.couchdb-02-lim-05" },
            listOf(Tag.of(MetricsTags.NAMESPACE.tagName, "couchdb")),
            listOf(
                VariableTag(MetricsTags.TCP_PORT) { "444" },
                VariableTag(MetricsTags.NODE_ID) { "couchdb-02-lim-05" }
            ),
            { 0.8 },
            byName
        )

        val generator = GaugeGenerator(
            { "http.service.health.check.${listOf(443,444,445).random()}.couchdb-01-${listOf("lim", "rbx")}-0${Random.nextInt(1,6)}" },
            listOf(Tag.of(MetricsTags.NAMESPACE.tagName, "couchdb")),
            listOf(
                VariableTag(MetricsTags.TCP_PORT) { listOf("443","444","445").random() },
            ),
            { 1.0 },
            byName
        )

        triggerGenerator.generate(4).forEach {
            probe.receiveMeter(it, registry)
        }
        generator.generate(40).forEach {
            probe.receiveMeter(it, registry)
        }
        probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
        fakeJiraAction.payloads.size shouldBe 1
        fakeJiraAction.payloads.first().ticketId shouldBe "HealthChecks-http.service.health.check.444.couchdb-02-lim-05"
    }

})