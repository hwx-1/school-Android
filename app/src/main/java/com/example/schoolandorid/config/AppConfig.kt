package com.example.schoolandorid.config

import androidx.compose.ui.graphics.Color

/**
 * 全局常量与接口配置（对齐鸿蒙端 AppConfig.ets）。
 * 真机联调时将 API_BASE_URL 改为电脑局域网地址或线上 HTTPS 地址；
 * Android 模拟器访问宿主机使用 10.0.2.2。
 */
object AppConfig {
    const val APP_VERSION = "1.0.0"
    const val BUILD_VERSION = "1"

    // 当前真机 / 模拟器联调电脑地址；服务端源码位于 /Users/zhihu/school/server。
    const val API_BASE_URL = "http://10.19.236.131:8080"
    const val REQUEST_TIMEOUT_MS = 15000L
    const val MAX_POST_IMAGES = 9
    const val MAX_POST_TAGS = 3
}

/** 与设计基线一致的色值（对齐鸿蒙端 AppColors）。 */
object AppColors {
    val PRIMARY = Color(0xFF2E6BE6)
    val PRIMARY_LIGHT = Color(0xFF5B8DEF)
    val PRIMARY_DISABLED = Color(0xFF9AB6EA)
    val PRIMARY_BG = Color(0xFFE8EFFC)
    val DANGER = Color(0xFFE5484D)
    val DANGER_BG = Color(0x14E5484D)
    val ORANGE = Color(0xFFFF6B00)
    val ORANGE_BG = Color(0x14FF6B00)
    val PAGE_BG = Color(0xFFF7F8FA)
    val CARD_BG = Color(0xFFFFFFFF)
    val TEXT_PRIMARY = Color(0xFF1A1A1A)
    val TEXT_SECONDARY = Color(0xFF6B7280)
    val DIVIDER = Color(0xFFE5E7EB)
    val SUCCESS = Color(0xFF0A8A4D)
    val SUCCESS_BG = Color(0x140A8A4D)
    val PURPLE = Color(0xFF7C5CFC)
    val BUBBLE_OTHER = Color(0xFFEFF3FA)
    val ICON_BG = Color(0xFFF3F4F8)
    val ACTION_BG = Color(0xFFF4F5F7)
    val LIKE_BG = Color(0x142E6BE6)
    val TAG_BG = Color(0x1A2E6BE6)
    val LEGAL_TIP_BG = Color(0xFFEDF3FF)
}
