package com.icure.monitoring.model

enum class MetricsTags(val tagName: String, val queryValue: String) {
    NAMESPACE("location.namespace", "location_namespace"),
    INSTANCE_ID("location.instance.id", "location_instance_id"),
    NODE_ID("location.node.id", "location_node_id"),
    METRIC("metric", "metric"),
    TYPE("type", "type"),
    BACKEND("backend", "backend"),
    PATH_CLASS("pathClass", "pathClass")
}