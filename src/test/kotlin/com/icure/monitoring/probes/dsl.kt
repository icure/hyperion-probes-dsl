package com.icure.monitoring.probes

import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.model.LogLevel
import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.probes.dsl.ActionConfig
import com.icure.monitoring.probes.dsl.GaugeValue
import com.icure.monitoring.probes.dsl.JiraActionConfig
import com.icure.monitoring.probes.dsl.LogActionConfig
import com.icure.monitoring.probes.dsl.MaxTrigger
import com.icure.monitoring.probes.dsl.TotalTime
import com.icure.monitoring.probes.dsl.Trigger
import com.icure.monitoring.probes.dsl.matches
import com.icure.monitoring.probes.dsl.probeConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration
import kotlin.random.Random

class ProbeSerializationTest : StringSpec({

    fun testJiraActionConfig(duration: Duration?) {
        val exampleDuration = JiraActionConfig().apply { autoCloseAfter = duration }
        val encoded = Json.encodeToString(exampleDuration)
        val decoded = Json.decodeFromString<JiraActionConfig>(encoded)
        decoded.autoCloseAfter shouldBe exampleDuration.autoCloseAfter
    }

    "Can serialize a jira action config" {
        testJiraActionConfig(Duration.ofDays(Random.nextLong(0, 300)))
    }

    "Can serialize a jira action config with null duration" {
        testJiraActionConfig(null)
    }

    "Can serialize a Jira action config with the generic serializer" {
        val exampleDuration = JiraActionConfig().apply { autoCloseAfter = Duration.ofDays(1) } as ActionConfig<ActionPayload>
        val encoded = Json.encodeToString(exampleDuration)
        val decoded = Json.decodeFromString<ActionConfig<ActionPayload>>(encoded)
    }

    "Can serialize a log action config" {
        val exampleDuration = LogActionConfig().apply { level = LogLevel.TRACE }
        val encoded = Json.encodeToString(exampleDuration)
        val decoded = Json.decodeFromString<LogActionConfig>(encoded)
        decoded.level shouldBe exampleDuration.level
    }

    "Can serialize metrics" {
        val gaugeMetric = GaugeValue()
        val encoded = Json.encodeToString(gaugeMetric)
        val decoded = Json.decodeFromString<GaugeValue>(encoded)
    }

    "Can serialize a trigger" {
        val trigger = MaxTrigger().apply {
            timeFrame = Duration.ofDays(1)
            activationCondition = Trigger.Companion.ActivationCondition.LESS_THAN
            threshold = Random.nextDouble()
            metric = GaugeValue()
        }
        val encoded = Json.encodeToString(trigger)
        val decoded = Json.decodeFromString<MaxTrigger>(encoded)
        println(decoded)
    }

    "Test" {
        val probe = probeConfig {
            probeId = "12345"
            description = "A probe"
            dataSource {
                registry {
                    registryId = "minutelyElasticProperties"
                }
            }
            trigger {
                average {
                    TotalTime over Duration.ofMinutes(1)
                } greaterThan 0.0
            }
            filter {
                (MetricsTags.BACKEND matches "backendA") and (MetricsTags.PATH_CLASS matches "pathB")
            }
            action {
                jira {}
            }
        }
        println(probe)
    }

})