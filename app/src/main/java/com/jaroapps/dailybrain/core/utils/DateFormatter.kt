package com.jaroapps.dailybrain.core.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Resolves an epoch timestamp to the calendar date in the supplied local time zone.
 * The default follows the device time zone, so a day changes exactly at local 00:00.
 */
fun localDateAtEpochMillis(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()

fun formatMillisToDate(epochMillis: Long): Int =
    localDateAtEpochMillis(epochMillis)
        .format(DateTimeFormatter.BASIC_ISO_DATE)
        .toInt()
