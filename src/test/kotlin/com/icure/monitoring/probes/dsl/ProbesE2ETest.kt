@file:Suppress("UNCHECKED_CAST")

package com.icure.monitoring.probes.dsl

import com.icure.monitoring.actions.Action
import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.actions.payload.JiraActionPayload
import com.icure.monitoring.actions.payload.LogActionPayload
import com.icure.monitoring.model.LogLevel
import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.probes.RegistryProbe
import com.icure.monitoring.probes.dsl.aggregators.aggregator
import com.icure.monitoring.probes.dsl.descriptors.NO_TAG
import com.icure.monitoring.probes.dsl.descriptors.byTag
import com.icure.monitoring.probes.dsl.extractors.CountOfDistributionSummary
import com.icure.monitoring.probes.dsl.extractors.DistributionSummaryExtractor
import com.icure.monitoring.probes.dsl.extractors.GaugeExtractor
import com.icure.monitoring.probes.dsl.filters.isEqualTo
import com.icure.monitoring.probes.dsl.filters.meterIsADistribution
import com.icure.monitoring.probes.dsl.filters.meterIsAGauge
import com.icure.monitoring.probes.dsl.filters.metricNameIs
import com.icure.monitoring.probes.dsl.utils.aggregateUsing
import com.icure.monitoring.probes.dsl.utils.and
import com.icure.monitoring.probes.dsl.utils.lastProducedBy
import com.icure.monitoring.test.fake.FakeClock
import com.icure.monitoring.test.fake.FakeDistributionSummary
import com.icure.monitoring.test.fake.FakeJiraAction
import com.icure.monitoring.test.generateGauge
import com.icure.monitoring.test.generateMeter
import com.icure.monitoring.test.over
import com.icure.monitoring.test.uuid
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Tag
import java.time.Duration

class ProbesE2ETest : StringSpec({

    run {
        val registryId = uuid()
        val meterName = uuid()
        val probeId = uuid()
        val threshold = 10.0

        fun generateProbe() = probe {
            this.probeId = probeId
            dataSource {
                registry {
                    this.registryId = registryId
                }
            }

            filter {
                meterIsAGauge() and metricNameIs(meterName)
            }

            group {
                listOf(byTag(MetricsTags.BACKEND))
            }

            max {
                3 lastProducedBy GaugeExtractor
            }

            compare { value, referenceValue ->  value > referenceValue }

            fixedThreshold { threshold }

            action {
                jira { value, threshold, descriptors ->
                    JiraActionPayload(
                        ticketId = probeId,
                        title = "$value-$threshold",
                        description = descriptors.first().v,
                        autoCloseAfter = null,
                        value = value,
                    )
                }
            }

            action {
                log { _, _, _ ->
                    LogActionPayload("Test", LogLevel.DEBUG)
                }
            }
        } as RegistryProbe

        "Probe 1 - Sad flow: correct value, wrong probe type" {
            val probe = generateProbe()
            val fakeJiraAction = FakeJiraAction()
            (0 until  3).forEach { _ ->
                probe.receiveMeter(generateGauge(meterName, value = 1.0), registryId)
                val distribution = FakeDistributionSummary()
                distribution.record(1000.0)
                probe.receiveMeter(distribution, registryId)
            }
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads.shouldBeEmpty()
        }

        "Probe 1 - Sad flow: Correct value, but the probe does not match the filter" {
            val probe = generateProbe()
            val fakeJiraAction = FakeJiraAction()
            (0 until  3).forEach { _ ->
                probe.receiveMeter(generateGauge(uuid(), value = 11.0), registryId)
                val distribution = FakeDistributionSummary()
                distribution.record(1000.0)
                probe.receiveMeter(distribution, registryId)
            }
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads.shouldBeEmpty()
        }

        "Probe 1 - Happy flow, no group found" {
            val probe = generateProbe()
            val fakeJiraAction = FakeJiraAction()
            (0 until  3).forEach { _ ->
                probe.receiveMeter(generateGauge(meterName, value = 11.0), registryId)
                val distribution = FakeDistributionSummary()
                distribution.record(1000.0)
                probe.receiveMeter(distribution, registryId)
            }
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads.size shouldBe 1
            fakeJiraAction.payloads.first() shouldBe JiraActionPayload(probeId, "11.0-$threshold", NO_TAG,null, value = 11.0)
        }

        "Probe 1 - Happy flow, multiple groups" {
            val probe = generateProbe()
            val fakeJiraAction = FakeJiraAction()
            val g1 = uuid()
            val g2 = uuid()
            (0 until  3).forEach { _ ->
                probe.receiveMeter(generateMeter(meterName, listOf(Tag.of(MetricsTags.BACKEND.tagName, g1)), value = 11.0), registryId)
                probe.receiveMeter(generateMeter(meterName, listOf(Tag.of(MetricsTags.BACKEND.tagName, g2)), value = 11.0), registryId)
                val distribution = FakeDistributionSummary()
                distribution.record(1000.0)
                probe.receiveMeter(distribution, registryId)
            }
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads shouldContainExactlyInAnyOrder listOf(
                JiraActionPayload(probeId, "11.0-$threshold", g1,null, value = 11.0),
                JiraActionPayload(probeId, "11.0-$threshold", g2,null, value = 11.0)
            )
        }

        "Probe 1 - Happy flow, multiple groups but only one triggered" {
            val probe = generateProbe()
            val fakeJiraAction = FakeJiraAction()
            val g1 = uuid()
            val g2 = uuid()
            (0 until  3).forEach { _ ->
                probe.receiveMeter(generateMeter(meterName, listOf(Tag.of(MetricsTags.BACKEND.tagName, g1)), value = 11.0), registryId)
                probe.receiveMeter(generateMeter(meterName, listOf(Tag.of(MetricsTags.BACKEND.tagName, g2)), value = 9.0), registryId)
                val distribution = FakeDistributionSummary()
                distribution.record(1000.0)
                probe.receiveMeter(distribution, registryId)
            }
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads shouldContainExactlyInAnyOrder listOf(JiraActionPayload(probeId, "11.0-$threshold", g1,null, value = 11.0))
        }
    }

    run {
        val registryId = uuid()
        val meterName = uuid()
        val probeId = uuid()
        val outlierValue = 4.0
        val baselineValue = 1.0
        val baselineCount = 10

        fun generateProbe(clock: FakeClock, windowSize: Duration): RegistryProbe {
            val probe = probe {
                this.probeId = probeId
                dataSource {
                    registry {
                        this.registryId = registryId
                    }
                }

                filter {
                    meterIsADistribution() and metricNameIs(meterName)
                }

                group {
                    byTag(MetricsTags.BACKEND) and byTag(MetricsTags.HEALTH)
                }

                average {
                    DistributionSummaryExtractor.over(windowSize, clock)
                }

                compare { value, referenceValue ->
                    value > (referenceValue * 3)
                }

                threshold {
                    dataSource {
                        registry {
                            this.registryId = registryId
                        }
                    }

                    filter {
                        meterIsADistribution()
                    }

                    average {
                        DistributionSummaryExtractor.over(windowSize, clock)
                    }
                }

                action {
                    jira { value, threshold, descriptors ->
                        JiraActionPayload(
                            ticketId = probeId,
                            title = "$value-$threshold",
                            description = descriptors.joinToString("-"){ it.v },
                            autoCloseAfter = null,
                            value = value
                        )
                    }
                }

                action {
                    log { _, _, _ ->
                        LogActionPayload("Test", LogLevel.DEBUG)
                    }
                }
            } as RegistryProbe
            return probe
        }

        "Probe 2 - Sad flow: correct value, wrong probe type" {
            val duration = Duration.ofMinutes(1)
            val clock = FakeClock()
            val probe = generateProbe(clock, duration)
            val fakeJiraAction = FakeJiraAction()
            (0 until  baselineCount).forEach { _ ->
                probe.receiveMeter(generateGauge(meterName, value = outlierValue), registryId)
                probe.receiveMeter(generateGauge(value = baselineValue), registryId)
            }
            clock.advance(duration.toMillis())
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads.shouldBeEmpty()
        }

        "Probe 2 - Sad flow: Correct value, but the probe does not match the filter" {
            val duration = Duration.ofMinutes(1)
            val clock = FakeClock()
            val probe = generateProbe(clock, duration)
            val fakeJiraAction = FakeJiraAction()
            val dist = FakeDistributionSummary()
            val baseline = List(10) { FakeDistributionSummary("${uuid()}-$it")}
            (0 until  baselineCount).forEach { _ ->
                dist.record(outlierValue)
                baseline.forEach {
                    it.record(baselineValue)
                }
            }
            probe.receiveMeter(dist, registryId)
            baseline.forEach { probe.receiveMeter(it, registryId) }
            clock.advance(duration.toMillis())
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads.shouldBeEmpty()
        }

        "Probe 2 - Happy flow, no group found" {
            val threshold = (outlierValue + (baselineCount * baselineValue)) / (baselineCount + 1)
            val duration = Duration.ofMinutes(1)
            val clock = FakeClock()
            val probe = generateProbe(clock, duration)
            val fakeJiraAction = FakeJiraAction()
            val dist = FakeDistributionSummary(meterName)
            val baseline = List(10) { FakeDistributionSummary("${uuid()}-$it")}
            (0 until  baselineCount).forEach { _ ->
                dist.record(outlierValue)
                baseline.forEach {
                    it.record(baselineValue)
                }
            }
            probe.receiveMeter(dist, registryId)
            baseline.forEach { probe.receiveMeter(it, registryId) }
            clock.advance(duration.toMillis())
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads shouldContainExactlyInAnyOrder listOf(
                JiraActionPayload(probeId, "$outlierValue-$threshold", "$NO_TAG-$NO_TAG",null, value = outlierValue)
            )
        }

        "Probe 2 - Happy flow, multiple groups" {
            val threshold = (outlierValue + outlierValue + (baselineCount * 2 * baselineValue)) / ((baselineCount * 2) + 2)
            val duration = Duration.ofMinutes(1)
            val clock = FakeClock()
            val probe = generateProbe(clock, duration)
            val fakeJiraAction = FakeJiraAction()
            val backend = uuid()
            val h1 = uuid()
            val h2 = uuid()
            val dist1 = FakeDistributionSummary(meterName, listOf(Tag.of(MetricsTags.BACKEND.tagName, backend), Tag.of(MetricsTags.HEALTH.tagName, h1)))
            val dist2 = FakeDistributionSummary(meterName, listOf(Tag.of(MetricsTags.HEALTH.tagName, h2)))
            val baseline = List(baselineCount * 2) { FakeDistributionSummary("${uuid()}-$it")}
            (0 until  baselineCount).forEach { _ ->
                dist1.record(outlierValue)
                dist2.record(outlierValue)
                baseline.forEach {
                    it.record(baselineValue)
                }
            }
            probe.receiveMeter(dist1, registryId)
            probe.receiveMeter(dist2, registryId)
            baseline.forEach { probe.receiveMeter(it, registryId) }
            clock.advance(duration.toMillis())
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads shouldContainExactlyInAnyOrder listOf(
                JiraActionPayload(probeId, "$outlierValue-$threshold", "$backend-$h1",null, value = outlierValue),
                JiraActionPayload(probeId, "$outlierValue-$threshold", "$NO_TAG-$h2",null, value = outlierValue)
            )
        }

        "Probe 2 - Happy flow, multiple groups but only one triggered" {
            val threshold = (outlierValue + (outlierValue /2) + (baselineCount * 2 * baselineValue)) / ((baselineCount * 2) + 2)
            val duration = Duration.ofMinutes(1)
            val clock = FakeClock()
            val probe = generateProbe(clock, duration)
            val fakeJiraAction = FakeJiraAction()
            val backend = uuid()
            val h1 = uuid()
            val h2 = uuid()
            val dist1 = FakeDistributionSummary(meterName, listOf(Tag.of(MetricsTags.BACKEND.tagName, backend), Tag.of(MetricsTags.HEALTH.tagName, h1)))
            val dist2 = FakeDistributionSummary(meterName, listOf(Tag.of(MetricsTags.HEALTH.tagName, h2)))
            val baseline = List(baselineCount * 2) { FakeDistributionSummary("${uuid()}-$it")}
            (0 until  baselineCount).forEach { _ ->
                dist1.record(outlierValue)
                dist2.record(outlierValue / 2 )
                baseline.forEach {
                    it.record(baselineValue)
                }
            }
            probe.receiveMeter(dist1, registryId)
            probe.receiveMeter(dist2, registryId)
            baseline.forEach { probe.receiveMeter(it, registryId) }
            clock.advance(duration.toMillis())
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads shouldContainExactlyInAnyOrder listOf(
                JiraActionPayload(probeId, "$outlierValue-$threshold", "$backend-$h1",null, value = outlierValue),
            )
        }
    }

    run {
        val registryId = uuid()
        val meterName = uuid()
        val probeId = uuid()
        val healthId = uuid()
        val triggerId = uuid()

        fun generateProbe(clock: FakeClock, windowSize: Duration): RegistryProbe {
            val probe = probe {
                this.probeId = probeId
                dataSource {
                    registry {
                        this.registryId = registryId
                    }
                }

                filter {
                    meterIsADistribution() and metricNameIs(meterName)
                }

                group {
                    listOf(byTag(MetricsTags.BACKEND))
                }

                customAggregation {
                    CountOfDistributionSummary.over(windowSize, clock) aggregateUsing aggregator {
                        it.getValues()?.sum()
                    }
                }

                compare { value, referenceValue ->
                    value > referenceValue
                }

                threshold {
                    dataSource {
                        registry {
                            this.registryId = triggerId
                        }
                    }

                    filter {
                        meterIsAGauge() and (MetricsTags.HEALTH isEqualTo healthId)
                    }

                    max {
                        5 lastProducedBy GaugeExtractor
                    }
                }

                action {
                    jira { value, threshold, descriptors ->
                        JiraActionPayload(
                            ticketId = probeId,
                            title = "$value-$threshold",
                            description = descriptors.joinToString("-"){ it.v },
                            autoCloseAfter = null,
                            value = value
                        )
                    }
                }

                action {
                    log { _, _, _ ->
                        LogActionPayload("Test", LogLevel.DEBUG)
                    }
                }
            } as RegistryProbe
            return probe
        }

        "Probe 3 - Sad flow: correct value, wrong probe type" {
            val clock = FakeClock()
            val windowSize = Duration.ofSeconds(600)
            val probe = generateProbe(clock, windowSize)
            val fakeJiraAction = FakeJiraAction()
            val distribution = FakeDistributionSummary()
            distribution.record(1000.0)
            (0 until 10).forEach { _ ->
                probe.receiveMeter(generateGauge(meterName, value = 10.0), registryId)
                probe.receiveMeter(generateGauge(value = 10.0), triggerId)
            }
            clock.advance(windowSize.toMillis())
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads.shouldBeEmpty()
        }

        "Probe 3 - Sad flow: Correct value, but the probe does not match the filter" {
            val clock = FakeClock()
            val windowSize = Duration.ofSeconds(600)
            val probe = generateProbe(clock, windowSize)
            val fakeJiraAction = FakeJiraAction()
            val dist = FakeDistributionSummary()
            (0 until 10).forEach { _ ->
                dist.record(1.0)
                probe.receiveMeter(generateGauge(value = 11.0), triggerId)
            }
            probe.receiveMeter(dist, registryId)
            clock.advance(windowSize.toMillis())
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads.shouldBeEmpty()
        }

        "Probe 3 - Happy flow, no group found" {
            val clock = FakeClock()
            val windowSize = Duration.ofSeconds(600)
            val probe = generateProbe(clock, windowSize)
            val fakeJiraAction = FakeJiraAction()
            val dist = FakeDistributionSummary(meterName)
            (0 until 10).forEach { _ ->
                dist.record(1.0)
                probe.receiveMeter(generateGauge(tags = listOf(Tag.of(MetricsTags.HEALTH.tagName, healthId)), value = 1.0), triggerId)
            }
            probe.receiveMeter(dist, registryId)
            clock.advance(windowSize.toMillis())
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads shouldContainExactlyInAnyOrder listOf(
                JiraActionPayload(probeId, "10.0-1.0", NO_TAG,null, value = 10.0)
            )
        }

        "Probe 3 - Happy flow, multiple groups" {
            val clock = FakeClock()
            val windowSize = Duration.ofSeconds(600)
            val probe = generateProbe(clock, windowSize)
            val fakeJiraAction = FakeJiraAction()
            val h1 = uuid()
            val h2 = uuid()
            val dist1 = FakeDistributionSummary(meterName, listOf(Tag.of(MetricsTags.BACKEND.tagName, h1)))
            val dist2 = FakeDistributionSummary(meterName, listOf(Tag.of(MetricsTags.BACKEND.tagName, h2)))
            (0 until 10).forEach { _ ->
                dist1.record(1.0)
                dist2.record(1.0)
                probe.receiveMeter(generateGauge(tags = listOf(Tag.of(MetricsTags.HEALTH.tagName, healthId)), value = 1.0), triggerId)
            }
            probe.receiveMeter(dist1, registryId)
            probe.receiveMeter(dist2, registryId)
            clock.advance(windowSize.toMillis())
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads shouldContainExactlyInAnyOrder listOf(
                JiraActionPayload(probeId, "10.0-1.0", h1,null, value = 10.0),
                JiraActionPayload(probeId, "10.0-1.0", h2,null, value = 10.0)
            )
        }

        "Probe 3 - Happy flow, multiple groups but only one triggered" {
            val clock = FakeClock()
            val windowSize = Duration.ofSeconds(600)
            val probe = generateProbe(clock, windowSize)
            val fakeJiraAction = FakeJiraAction()
            val h1 = uuid()
            val h2 = uuid()
            val dist1 = FakeDistributionSummary(meterName, listOf(Tag.of(MetricsTags.BACKEND.tagName, h1)))
            val dist2 = FakeDistributionSummary(meterName, listOf(Tag.of(MetricsTags.BACKEND.tagName, h2)))
            (0 until 10).forEach { _ ->
                dist1.record(1.0)
                dist2.record(1.0)
                probe.receiveMeter(generateGauge(tags = listOf(Tag.of(MetricsTags.HEALTH.tagName, healthId)), value = 12.0), triggerId)
            }
            (0 .. 5).forEach { _ ->
                dist1.record(1.0)
            }
            probe.receiveMeter(dist1, registryId)
            probe.receiveMeter(dist2, registryId)
            clock.advance(windowSize.toMillis())
            probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
            fakeJiraAction.payloads shouldContainExactlyInAnyOrder listOf(
                JiraActionPayload(probeId, "16.0-12.0", h1,null, value = 16.0)
            )
        }
    }

})
