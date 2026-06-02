package com.crimeafuel.app.util

import java.util.concurrent.TimeUnit

object DateUtils {
    fun formatTimeAgo(timestampMillis: Long): String {
        if (timestampMillis == 0L) return "нет данных"

        val now = System.currentTimeMillis()
        val diff = now - timestampMillis

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "только что"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes мин. назад"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours ч. назад"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days дн. назад"
            }
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days дн. назад"
            }
        }
    }
}
