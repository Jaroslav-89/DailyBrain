package com.jaroapps.dailybrain.core.utils

fun formatCountingTime(durationMs: Long): String {
    val totalTenths = durationMs.coerceAtLeast(0L) / 100L
    val minutes = totalTenths / 600L
    val seconds = (totalTenths % 600L) / 10L
    val tenths = totalTenths % 10L

    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}