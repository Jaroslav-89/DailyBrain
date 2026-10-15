package com.jaroapps.dailybrain.core.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatMillisToDate(millis: Long): Int {
    val formatter = DateTimeFormatter.ofPattern("YYYYMMdd")
        .withLocale(Locale.ENGLISH)

    val date = Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    return formatter.format(date).toInt()
}
