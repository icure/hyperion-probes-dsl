package com.icure.monitoring.probes.dsl.descriptors

import com.icure.monitoring.model.MetricsTags

const val NO_TAG = "NO_TAG"

/**
 * Creates a [Descriptor] that will return the value of the tag passed as parameter, or [NO_TAG] if the meter does not
 * have that tag.
 *
 * @param tag a [MetricsTags].
 * @return a [Descriptor].
 */
fun byTag(tag: MetricsTags): Descriptor = { meter ->
    DescriptorElement(
        tag.tagName,
        meter.id.tags.firstOrNull { it.key == tag.tagName }?.value ?: NO_TAG
    )
}