package com.icure.monitoring.probes.dsl.descriptors

import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.test.generateMeter
import com.icure.monitoring.test.uuid
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag

class DescriptorTest : StringSpec({

    "the byName descriptor can extract the name from a meter" {
        val name = uuid()
        val meter = generateMeter(name)

        byName(meter) shouldBe name
    }

    "the byTag descriptor can extract the tag value from a meter" {
        val tagType = MetricsTags.NODE_ID
        val tagValue = uuid()
        val meter = generateMeter(tags = listOf(Tag.of(tagType.tagName, tagValue)))

        byTag(tagType)(meter) shouldBe tagValue
    }

    "the byTag descriptor will return the default value if the specified tag is not present" {
        val meter = generateMeter()

        byTag(MetricsTags.METRIC)(meter) shouldBe NO_TAG
    }

    "is it possible to define custom descriptors" {
        val customDescriptor = descriptor {
            "${it.id.name}&${it.id.type}"
        }
        val name = uuid()
        val type = Meter.Type.DISTRIBUTION_SUMMARY
        val meter = generateMeter(name, type = type)

        customDescriptor(meter) shouldBe "$name&$type"
    }

})