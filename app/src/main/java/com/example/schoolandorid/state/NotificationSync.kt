package com.example.schoolandorid.state

import android.util.Log
import com.example.schoolandorid.net.Api
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "NotificationSync"
private const val POLL_INTERVAL_MS = 3000L

/**
 * 前台通知同步（对齐鸿蒙端 NotificationSync.ets）：
 * 服务端产生点赞/评论通知后，在一个轮询周期内刷新消息列表与底栏红点。
 */
object NotificationSync {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    @Volatile
    private var syncing = false

    fun start() {
        if (job != null) return
        job = scope.launch {
            syncNow()
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                syncNow()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    suspend fun syncNow() {
        if (syncing || !AppState.loggedIn || !AppState.notificationAutoSyncEnabled) return
        syncing = true
        var changed = false
        try {
            val response = Api.notifications()
            val latestId = response.items.maxOfOrNull { it.id } ?: 0L
            changed = AppState.syncNotificationSummary(response.unread, latestId) || changed
        } catch (e: Exception) {
            // 网络异常时保留现有未读状态，下一个轮询周期自动重试
            Log.e(TAG, "syncNow failed: ${e.message}")
        }
        try {
            val response = Api.listDirectConversations()
            changed = AppState.syncDirectConversations(response.items, response.unread) || changed
        } catch (e: Exception) {
            Log.e(TAG, "sync direct messages failed: ${e.message}")
        }
        if (changed) {
            AppState.publishMessageChange()
        }
        syncing = false
    }
}
