package com.example.schoolandorid.state

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.schoolandorid.model.Account
import com.example.schoolandorid.model.DirectConversationItem
import com.example.schoolandorid.model.DirectMessage
import com.example.schoolandorid.net.Api
import com.example.schoolandorid.net.ApiError
import com.example.schoolandorid.net.CookieJar

/**
 * 全局会话与跨页面轻量状态信号（对齐鸿蒙端 Session.ets + AppState.ets）。
 * 帖子/消息写操作成功后递增版本号，各页面观察版本号重新拉取。
 */
object AppState {
    // ---- 会话 ----
    var account by mutableStateOf<Account?>(null)
        private set
    var loggedIn by mutableStateOf(false)
        private set

    // ---- 刷新信号（私有 backing + 公开只读，避免 JVM setter 签名冲突）----
    private var _postRevision by mutableIntStateOf(0)
    val postRevision: Int get() = _postRevision

    private var _messageRevision by mutableIntStateOf(0)
    val messageRevision: Int get() = _messageRevision

    private var _unreadCount by mutableIntStateOf(0)
    val unreadCount: Int get() = _unreadCount

    private var _directUnreadCount by mutableIntStateOf(0)
    val directUnreadCount: Int get() = _directUnreadCount

    private var _latestNotificationId by mutableLongStateOf(0L)
    val latestNotificationId: Long get() = _latestNotificationId

    private var _latestDirectMessageId by mutableLongStateOf(0L)
    val latestDirectMessageId: Long get() = _latestDirectMessageId

    /** 服务端私信会话快照：使底栏数字和消息卡片在同一个状态变更中更新。 */
    private var directConversations by mutableStateOf<List<DirectConversationItem>>(emptyList())
    private var directSnapshotReady by mutableStateOf(false)

    // ---- 本机偏好（SharedPreferences 持久化，对齐鸿蒙端 PersistentStorage）----
    private lateinit var prefs: SharedPreferences

    private var _privacyAgreed by mutableStateOf(false)
    var privacyAgreed: Boolean
        get() = _privacyAgreed
        set(value) {
            _privacyAgreed = value
            if (::prefs.isInitialized) prefs.edit().putBoolean("privacy_agreed", value).apply()
        }

    private var _inAppNotificationEnabled by mutableStateOf(true)
    var inAppNotificationEnabled: Boolean
        get() = _inAppNotificationEnabled
        set(value) {
            _inAppNotificationEnabled = value
            if (::prefs.isInitialized) prefs.edit().putBoolean("in_app_notification_enabled", value).apply()
        }

    private var _notificationBadgeEnabled by mutableStateOf(true)
    var notificationBadgeEnabled: Boolean
        get() = _notificationBadgeEnabled
        set(value) {
            _notificationBadgeEnabled = value
            if (::prefs.isInitialized) prefs.edit().putBoolean("notification_badge_enabled", value).apply()
        }

    private var _notificationAutoSyncEnabled by mutableStateOf(true)
    var notificationAutoSyncEnabled: Boolean
        get() = _notificationAutoSyncEnabled
        set(value) {
            _notificationAutoSyncEnabled = value
            if (::prefs.isInitialized) prefs.edit().putBoolean("notification_auto_sync_enabled", value).apply()
        }

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences("xsnbb_prefs", Context.MODE_PRIVATE)
        _privacyAgreed = prefs.getBoolean("privacy_agreed", false)
        _inAppNotificationEnabled = prefs.getBoolean("in_app_notification_enabled", true)
        _notificationBadgeEnabled = prefs.getBoolean("notification_badge_enabled", true)
        _notificationAutoSyncEnabled = prefs.getBoolean("notification_auto_sync_enabled", true)
    }

    // ---- 信号 ----
    fun publishPostChange() {
        _postRevision += 1
    }

    fun requestPostRefresh() = publishPostChange()

    fun publishMessageChange() {
        _messageRevision += 1
    }

    fun requestMessageRefresh() = publishMessageChange()

    fun setUnreadCount(count: Int) {
        val normalized = maxOf(0, count)
        if (_unreadCount != normalized) _unreadCount = normalized
    }

    /** 私信未读数：与通知未读分开存储，底栏冒泡展示两者合计。 */
    fun setDirectUnreadCount(count: Int) {
        val normalized = maxOf(0, count)
        if (_directUnreadCount != normalized) _directUnreadCount = normalized
    }

    /** 同步通知摘要；未读数或最新通知变化时返回 true。 */
    fun syncNotificationSummary(count: Int, latestId: Long): Boolean {
        val normalizedCount = maxOf(0, count)
        val normalizedLatestId = maxOf(0L, latestId)
        val changed = _unreadCount != normalizedCount || _latestNotificationId != normalizedLatestId
        _unreadCount = normalizedCount
        _latestNotificationId = normalizedLatestId
        return changed
    }

    /** 同步私信摘要；未读数或最新消息变化时返回 true。 */
    fun syncDirectSummary(count: Int, latestMessageId: Long): Boolean {
        val normalizedCount = maxOf(0, count)
        val normalizedLatestId = maxOf(0L, latestMessageId)
        val changed = _directUnreadCount != normalizedCount || _latestDirectMessageId != normalizedLatestId
        _directUnreadCount = normalizedCount
        _latestDirectMessageId = normalizedLatestId
        return changed
    }

    /** 保存服务端会话快照，使底栏数字和消息卡片在同一个状态变更中更新。 */
    fun syncDirectConversations(items: List<DirectConversationItem>, count: Int): Boolean {
        var latestMessageId = 0L
        items.forEach { conversation ->
            conversation.messages.forEach { message ->
                latestMessageId = maxOf(latestMessageId, message.id)
            }
        }
        directConversations = items.toList()
        directSnapshotReady = true
        return syncDirectSummary(count, latestMessageId)
    }

    fun hasDirectSnapshot(): Boolean = directSnapshotReady

    fun directConversationSnapshot(): List<DirectConversationItem> = directConversations.toList()

    /** 发送接口响应即写入卡片快照，不等待下一次网络刷新。 */
    fun appendDirectMessage(conversationId: Long, message: DirectMessage, unlocked: Boolean) {
        if (!directSnapshotReady) return
        var updated: DirectConversationItem? = null
        val remaining = mutableListOf<DirectConversationItem>()
        directConversations.forEach { conversation ->
            if (conversation.id != conversationId) {
                remaining.add(conversation)
                return@forEach
            }
            val exists = conversation.messages.any { it.id == message.id }
            updated = conversation.copy(
                unlocked = unlocked,
                messages = if (exists) conversation.messages else conversation.messages + message,
                updated_at = message.created_at,
            )
        }
        updated?.let { directConversations = listOf(it) + remaining }
    }

    /** 会话已读：快照中该会话未读清零，同时更新全量私信未读数。 */
    fun markDirectConversationRead(conversationId: Long, unread: Int) {
        if (directSnapshotReady) {
            directConversations = directConversations.map { conversation ->
                if (conversation.id != conversationId) conversation
                else conversation.copy(unread_count = 0)
            }
        }
        setDirectUnreadCount(unread)
    }

    // ---- 会话（对齐鸿蒙端 Session.ets）----
    fun applyAccount(value: Account) {
        account = value
        loggedIn = true
    }

    /** 冷启动时尝试用已有 Cookie 恢复登录态（Cookie 为内存会话，进程重启后需重新登录）。 */
    suspend fun restore(): Boolean {
        if (!CookieJar.hasSession()) {
            clearSession()
            return false
        }
        return try {
            val resp = Api.me()
            applyAccount(resp.account)
            true
        } catch (err: Exception) {
            if (err is ApiError && (err.status == 401 || err.status == 403)) {
                clearSession()
                false
            } else {
                loggedIn
            }
        }
    }

    fun clearSession() {
        CookieJar.clear()
        account = null
        loggedIn = false
        _unreadCount = 0
        _directUnreadCount = 0
        _latestNotificationId = 0L
    }

    suspend fun logout() {
        try {
            Api.logout()
        } catch (_: Exception) {
            // 忽略登出接口异常，本地态必须清理
        }
        clearSession()
    }
}
