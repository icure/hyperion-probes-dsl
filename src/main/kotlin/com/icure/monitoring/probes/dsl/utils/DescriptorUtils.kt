package com.icure.monitoring.probes.dsl.utils

import com.icure.monitoring.probes.dsl.descriptors.Descriptor

infix fun Descriptor.and(other: Descriptor) = listOf(this, other)
infix fun List<Descriptor>.and(other: Descriptor) = this + other
infix fun Descriptor.and(others: List<Descriptor>) = listOf(this) + others