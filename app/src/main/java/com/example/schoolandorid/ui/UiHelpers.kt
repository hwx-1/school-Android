package com.example.schoolandorid.ui

import android.content.Context
import android.widget.Toast

/** Toast 统一入口（对齐鸿蒙端各页面的 toast()）。 */
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/** 从异常提取用户可读的错误文案。 */
fun Throwable.errorMessage(fallback: String): String {
    return if (this is com.example.schoolandorid.net.ApiError) this.message ?: fallback else fallback
}
