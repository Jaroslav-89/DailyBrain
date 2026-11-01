package com.jaroapps.dailybrain.core.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatMillisToDate(epochMillis: Long): Int {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.BASIC_ISO_DATE)
        .toInt()
}