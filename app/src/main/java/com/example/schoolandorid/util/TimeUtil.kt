package com.example.schoolandorid.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 相对时间格式化（对齐鸿蒙端 TimeUtil.ets）。 */
object TimeUtil {
    fun format(iso: String): String {
        val time = parseIso(iso) ?: return ""
        val diff = System.currentTimeMillis() - time
        val minute = 60_000L
        if (diff < minute) return "刚刚"
        if (diff < 60 * minute) return "${diff / minute} 分钟前"
        if (diff < 24 * 60 * minute) return "${diff / (60 * minute)} 小时前"
        if (diff < 7 * 24 * 60 * minute) return "${diff / (24 * 60 * minute)} 天前"
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        return "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
    }

    private fun parseIso(iso: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date: Date? = sdf.parse(iso)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return null
    }
}
