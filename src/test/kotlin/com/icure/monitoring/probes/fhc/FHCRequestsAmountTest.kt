package com.icure.monitoring.probes.fhc

import com.icure.monitoring.actions.Action
import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.actions.payload.JiraActionPayload
import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.probes.RegistryProbe
import com.icure.monitoring.probes.dsl.aggregators.aggregator
import com.icure.monitoring.probes.dsl.collectors.TimeWindowCollector
import com.icure.monitoring.probes.dsl.descriptors.byTag
import com.icure.monitoring.probes.dsl.extractors.CountOfDistributionSummary
import com.icure.monitoring.probes.dsl.extractors.DistributionSummaryExtractor
import com.icure.monitoring.probes.dsl.filters.GenericMeterFilter
import com.icure.monitoring.probes.dsl.filters.isEqualTo
import com.icure.monitoring.probes.dsl.filters.meterIsADistribution
import com.icure.monitoring.probes.dsl.probe
import com.icure.monitoring.probes.dsl.utils.aggregateUsing
import com.icure.monitoring.test.fake.FakeClock
import com.icure.monitoring.test.fake.FakeJiraAction
import com.icure.monitoring.test.generators.DistributionSummaryGenerator
import com.icure.monitoring.test.generators.VariableTag
import com.icure.monitoring.test.overWithFakeClock
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Tag
import org.apache.commons.rng.sampling.DiscreteProbabilityCollectionSampler
import org.apache.commons.rng.simple.RandomSource
import java.time.Duration
import kotlin.random.Random

@Suppress("UNCHECKED_CAST")
class FHCRequestsAmountTest : StringSpec({

    "FHCRequestsAmount test" {
        val clock = FakeClock()
        val fakeJiraAction = FakeJiraAction()
        val probe = probe {
            this.probeId = "fhc_number_of_requests_per_node"
            dataSource {
                registry {
                    registryId = "minutelyLogsElasticProperties"
                }
            }

            filter {
                meterIsADistribution() and GenericMeterFilter("fhc") { meter ->
                    meter.id.tags.firstOrNull {
                        it.key == MetricsTags.COMPONENT.tagName
                    }?.value?.lowercase() == "fhc"
                } and (MetricsTags.METRIC isEqualTo "totalTime")
            }

            group {
                listOf(byTag(MetricsTags.BACKEND))
            }

            count {
                DistributionSummaryExtractor.overWithFakeClock(Duration.ofMinutes(1), clock)
            }

            threshold {
                dataSource {
                    registry {
                        registryId = "minutelyLogsElasticProperties"
                    }
                }

                filter {
                    meterIsADistribution() and GenericMeterFilter("fhc") { meter ->
                        meter.id.tags.firstOrNull {
                            it.key == MetricsTags.COMPONENT.tagName
                        }?.value?.lowercase() == "fhc"
                    } and (MetricsTags.METRIC isEqualTo "totalTime")
                }

                customAggregation {
                    CountOfDistributionSummary.overWithFakeClock(Duration.ofMinutes(5), clock) aggregateUsing aggregator {
                        (it as TimeWindowCollector).sum()?.div((5 * 18))
                    }
                }

            }

            compare { value, referenceValue -> value > (referenceValue * 3) }

            action {
                jira { value, threshold, descriptors ->
                    val node = descriptors.firstOrNull()?.v ?: "UNKNOWN"
                    JiraActionPayload(
                        ticketId = "fhc_number_of_requests_per_node_on_$node",
                        title = "Too many requests on FHC $node ($value)",
                        description = "Number of requests is $value, average of all node over the last 5 minutes is: $threshold",
                        autoCloseAfter = Duration.ofMinutes(30).toMillis()
                    )
                }
            }
        } as RegistryProbe

        val generator = DistributionSummaryGenerator(
            "FHC_haproxy_log_totalTime",
            listOf(Tag.of(MetricsTags.COMPONENT.tagName, "FHC"), Tag.of(MetricsTags.METRIC.tagName, "totalTime")),
            listOf(VariableTag(MetricsTags.BACKEND) {
                listOf("rbx-01", "rbx-02", "rbx-03").random().let {
                    "fhc-01-$it-${Random.nextInt(1, 7)}"
                }
            }),
            { DiscreteProbabilityCollectionSampler(
                RandomSource.XO_RO_SHI_RO_128_PP.create(),
                mapOf(1L to 32.4, 2L to 11.2, 3L to 9.3, 4L to 6.1, 5L to 5.7, 6L to 5.4, 7L to 5.1, 8L to 4.3, 9L to 2.9, 10L to 2.4)
            ).sample() },
            { DiscreteProbabilityCollectionSampler(
                RandomSource.XO_RO_SHI_RO_128_PP.create(),
                mapOf(1.0 to 10.8, 2.0 to 5.8, 0.0 to 4.9, 3.0 to 4.6, 4.0 to 4.1, 7.0 to 2.5, 5.0 to 2.2, 9.0 to 1.7, 6.0 to 1.6, 10.0 to 1.3)
            ).sample() },
            { DiscreteProbabilityCollectionSampler(
                RandomSource.XO_RO_SHI_RO_128_PP.create(),
                mapOf(1.0 to 20.0, 2.0 to 17.7, 0.0 to 4.9, 3.0 to 1.2, 20.0 to 1.1, 7.0 to 0.7, 4.0 to 0.6, 5.0 to 0.6, 8.0 to 0.5, 17.0 to 0.5)
            ).sample() },
            byTag(MetricsTags.BACKEND)
        )
        (0 until 5).forEach { _ ->
            generator.generate(900).forEach {
                probe.receiveMeter(it, "minutelyLogsElasticProperties")
            }
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads.shouldBeEmpty()
            clock.advance(60_000)
        }

        val triggerGenerator = DistributionSummaryGenerator(
            "FHC_haproxy_log_totalTime",
            listOf(Tag.of(MetricsTags.COMPONENT.tagName, "FHC"), Tag.of(MetricsTags.METRIC.tagName, "totalTime")),
            listOf(VariableTag(MetricsTags.BACKEND) { "fhc-01-rbx-01-1" }),
            { DiscreteProbabilityCollectionSampler(
                RandomSource.XO_RO_SHI_RO_128_PP.create(),
                mapOf(4L to 32.4, 8L to 11.2, 12L to 9.3, 16L to 6.1, 20L to 5.7, 24L to 5.4, 28L to 5.1, 32L to 4.3, 36L to 2.9, 40L to 2.4)
            ).sample() },
            { DiscreteProbabilityCollectionSampler(
                RandomSource.XO_RO_SHI_RO_128_PP.create(),
                mapOf(1.0 to 10.8, 2.0 to 5.8, 0.0 to 4.9, 3.0 to 4.6, 4.0 to 4.1, 7.0 to 2.5, 5.0 to 2.2, 9.0 to 1.7, 6.0 to 1.6, 10.0 to 1.3)
            ).sample() },
            { DiscreteProbabilityCollectionSampler(
                RandomSource.XO_RO_SHI_RO_128_PP.create(),
                mapOf(1.0 to 20.0, 2.0 to 17.7, 0.0 to 4.9, 3.0 to 1.2, 20.0 to 1.1, 7.0 to 0.7, 4.0 to 0.6, 5.0 to 0.6, 8.0 to 0.5, 17.0 to 0.5)
            ).sample() },
            byTag(MetricsTags.BACKEND)
        )
        triggerGenerator.generate(900).forEach {
            probe.receiveMeter(it, "minutelyLogsElasticProperties")
        }
        probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
        fakeJiraAction.payloads.size shouldBe 1
        fakeJiraAction.payloads.first().let {
            it.ticketId shouldBe "fhc_number_of_requests_per_node_on_fhc-01-rbx-01-1"
        }
    }

})
