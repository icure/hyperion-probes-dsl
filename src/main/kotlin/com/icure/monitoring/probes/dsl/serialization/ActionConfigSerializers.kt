package com.icure.monitoring.probes.dsl.serialization

import com.icure.monitoring.model.LogLevel
import com.icure.monitoring.probes.dsl.ActionConfig
import com.icure.monitoring.probes.dsl.JiraActionConfig
import com.icure.monitoring.probes.dsl.LogActionConfig
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

object GenericActionConfigSerializer : KSerializer<ActionConfig<*>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("action") {
        element<String>("type")
        element<String>("action")
    }

    override fun deserialize(decoder: Decoder): ActionConfig<*> = decoder.decodeStructure(descriptor) {
        var action: ActionConfig<*>? = null
        var type = ""
        do {
            val index = decodeElementIndex(descriptor)
            when(index) {
                0 -> { type = decodeStringElement(descriptor, 0) }
                1 -> {
                    action = when(type) {
                        JiraActionConfig::class.qualifiedName -> {
                            decodeSerializableElement(descriptor, 1, JiraActionConfigSerializer)
                        }

                        LogActionConfigSerializer::class.qualifiedName -> {
                            decodeSerializableElement(descriptor, 1, LogActionConfigSerializer)
                        }

                        else -> throw SerializationException("Unknown action type")
                    }
                }
            }
        } while (index != CompositeDecoder.DECODE_DONE)
        action ?: throw SerializationException("Cannot deserialize action")
    }

    override fun serialize(encoder: Encoder, value: ActionConfig<*>) = encoder.encodeStructure(descriptor) {
        when(value) {
            is JiraActionConfig -> {
                encodeStringElement(descriptor, 0, JiraActionConfig::class.qualifiedName ?: throw SerializationException("Cannot serialize JiraAction"))
                encodeSerializableElement(descriptor, 1, JiraActionConfigSerializer, value)
            }
            is LogActionConfig -> {
                encodeStringElement(descriptor, 0, LogActionConfigSerializer::class.qualifiedName ?: throw SerializationException("Cannot serialize LogAction"))
                encodeSerializableElement(descriptor, 1, LogActionConfigSerializer, value)
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
object JiraActionConfigSerializer : KSerializer<JiraActionConfig> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("jiraAction") {
        element<String?>("autoCloseAfter")
    }
    override fun deserialize(decoder: Decoder): JiraActionConfig = JiraActionConfig().apply {
        decoder.decodeStructure(descriptor) {
            do {
                val index = decodeElementIndex(descriptor)
                if(index == 0) {
                    autoCloseAfter = decodeNullableSerializableElement(descriptor, index, Duration.serializer())?.toJavaDuration()
                }
            } while (index != CompositeDecoder.DECODE_DONE)
        }
    }

    override fun serialize(encoder: Encoder, value: JiraActionConfig) = encoder.encodeStructure(descriptor) {
        encodeNullableSerializableElement(
            descriptor,
            0,
            String.serializer(),
            value.autoCloseAfter?.toKotlinDuration()?.toIsoString()
        )
    }
}

object LogActionConfigSerializer: KSerializer<LogActionConfig> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("logAction") {
        element<String>("level")
    }
    override fun deserialize(decoder: Decoder): LogActionConfig = LogActionConfig().apply {
        decoder.decodeStructure(descriptor) {
            do {
                val index = decodeElementIndex(descriptor)
                if(index == 0) {
                    level = LogLevel.valueOf(decodeStringElement(descriptor, index))
                }
            } while (index != CompositeDecoder.DECODE_DONE)
        }
    }

    override fun serialize(encoder: Encoder, value: LogActionConfig) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.level.name)
    }
}