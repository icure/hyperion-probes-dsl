package com.icure.monitoring.probes.dsl.serialization

import com.icure.monitoring.probes.dsl.AverageTrigger
import com.icure.monitoring.probes.dsl.CountTrigger
import com.icure.monitoring.probes.dsl.MaxTrigger
import com.icure.monitoring.probes.dsl.Metric
import com.icure.monitoring.probes.dsl.Trigger
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
import java.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

open class TriggerSerializer : KSerializer<Trigger> {

    private data class TriggerStub(
        val timeFrame: Duration? = null,
        val activationCondition: Trigger.Companion.ActivationCondition? = null,
        val threshold: Double? = null,
        val metric: Metric? = null
    )

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("trigger") {
        element<String>("timeFrame")
        element<String>("activationCondition")
        element<Double>("threshold")
        element<String>("metric")
        element<String>("type")
    }

    override fun deserialize(decoder: Decoder): Trigger = decoder.decodeStructure(descriptor) {
        var stub = TriggerStub()
        var type = ""
        do {
            val index = decodeElementIndex(descriptor)
            when(index) {
                0 -> { stub = stub.copy(timeFrame = decodeSerializableElement(descriptor, index, kotlin.time.Duration.serializer()).toJavaDuration()) }
                1 -> { stub = stub.copy(activationCondition = Trigger.Companion.ActivationCondition.valueOf(decodeStringElement(descriptor, index))) }
                2 -> { stub = stub.copy(threshold = decodeDoubleElement(descriptor, index)) }
                3 -> { stub = stub.copy(metric = decodeSerializableElement(descriptor, index, Metric.serializer())) }
                4 -> { type = decodeStringElement(descriptor, index)}
            }
        } while (index != CompositeDecoder.DECODE_DONE)
        when(type) {
            MaxTrigger::class.qualifiedName -> MaxTrigger().initTrigger(stub)
            AverageTrigger::class.qualifiedName -> AverageTrigger().initTrigger(stub)
            CountTrigger::class.qualifiedName -> CountTrigger().initTrigger(stub)
            else -> throw SerializationException("Unknown concrete trigger $type")
        }
    }

    private fun Trigger.initTrigger(stub: TriggerStub): Trigger {
        timeFrame = stub.timeFrame ?: throw SerializationException("Missing time frame field")
        activationCondition = stub.activationCondition ?: throw SerializationException("Missing activation condition field")
        threshold = stub.threshold ?: throw SerializationException("Missing threshold field")
        metric = stub.metric ?: throw SerializationException("Missing metric field")
        return this
    }

    override fun serialize(encoder: Encoder, value: Trigger) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.timeFrame.toKotlinDuration().toIsoString())
        encodeStringElement(descriptor, 1, value.activationCondition.name)
        encodeDoubleElement(descriptor, 2, value.threshold)
        encodeSerializableElement(descriptor, 3, Metric.serializer(), value.metric)
        encodeStringElement(descriptor, 4, value::class.qualifiedName ?: throw SerializationException("Unknown concrete Trigger class"))
    }
}