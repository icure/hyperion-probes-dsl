package com.icure.monitoring.probes.dsl.serialization

import com.icure.monitoring.probes.dsl.DataSource
import com.icure.monitoring.probes.dsl.ElasticDataSource
import com.icure.monitoring.probes.dsl.RegistryDataSource
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

object GenericDataSourceSerializer : KSerializer<DataSource> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("action") {
        element<String>("type")
        element<String>("action")
    }

    override fun deserialize(decoder: Decoder): DataSource = decoder.decodeStructure(descriptor) {
        var dataSource: DataSource? = null
        var type = ""
        do {
            val index = decodeElementIndex(descriptor)
            when(index) {
                0 -> { type = decodeStringElement(descriptor, 0) }
                1 -> {
                    dataSource = when(type) {
                        RegistryDataSource::class.qualifiedName -> {
                            decodeSerializableElement(descriptor, 1, RegistryDataSourceSerializer)
                        }
                        ElasticDataSource::class.qualifiedName -> {
                            decodeSerializableElement(descriptor, 1, ElasticDataSource.serializer())
                        }

                        else -> throw SerializationException("Unknown data source type")
                    }
                }
            }
        } while (index != CompositeDecoder.DECODE_DONE)
        dataSource ?: throw SerializationException("Cannot deserialize data source")
    }

    override fun serialize(encoder: Encoder, value: DataSource) = encoder.encodeStructure(descriptor) {
        when(value) {
            is RegistryDataSource -> {
                encodeStringElement(descriptor, 0, RegistryDataSource::class.qualifiedName ?: throw SerializationException("Cannot serialize RegistryDataSource"))
                encodeSerializableElement(descriptor, 1, RegistryDataSourceSerializer, value)
            }
            is ElasticDataSource -> {
                encodeStringElement(descriptor, 0, ElasticDataSource::class.qualifiedName ?: throw SerializationException("Cannot serialize ElasticDataSource"))
                encodeSerializableElement(descriptor, 1, ElasticDataSource.serializer(), value)
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
object RegistryDataSourceSerializer : KSerializer<RegistryDataSource> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("registryDataSource") {
        element<String>("registryId")
        element<String>("samplingWindow")
    }
    override fun deserialize(decoder: Decoder): RegistryDataSource = RegistryDataSource().apply {
        decoder.decodeStructure(descriptor) {
            do {
                val index = decodeElementIndex(descriptor)
                when(index) {
                    0 -> { registryId = decodeStringElement(descriptor, index) }
                    1 -> { samplingWindow = decodeSerializableElement(descriptor, index, Duration.serializer()).toJavaDuration() }
                }
            } while (index != CompositeDecoder.DECODE_DONE)
        }
    }

    override fun serialize(encoder: Encoder, value: RegistryDataSource) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.registryId)
        encodeNullableSerializableElement(
            descriptor,
            1,
            String.serializer(),
            value.samplingWindow.toKotlinDuration().toIsoString()
        )
    }
}