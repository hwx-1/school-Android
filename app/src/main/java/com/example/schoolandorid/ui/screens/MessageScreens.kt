package com.example.schoolandorid.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolandorid.config.AppColors
import com.example.schoolandorid.model.AppNotification
import com.example.schoolandorid.model.DirectMessage
import com.example.schoolandorid.nav.NavStack
import com.example.schoolandorid.nav.Route
import com.example.schoolandorid.net.Api
import com.example.schoolandorid.state.AppState
import com.example.schoolandorid.ui.components.EmptyState
import com.example.schoolandorid.ui.components.LoadingView
import com.example.schoolandorid.ui.components.MessageBubble
import com.example.schoolandorid.ui.components.NavBar
import com.example.schoolandorid.ui.components.NotificationItemView
import com.example.schoolandorid.ui.components.notificationTypeColor
import com.example.schoolandorid.ui.components.notificationTypeIcon
import com.example.schoolandorid.ui.errorMessage
import com.example.schoolandorid.ui.toast
import com.example.schoolandorid.util.TimeUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "MessageScreens"

/** 互动通知列表页（对齐鸿蒙端 pages/Notifications.ets）。 */
@Composable
fun NotificationsScreen(nav: NavStack) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messageRevision = AppState.messageRevision
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var unread by remember { mutableIntStateOf(0) }

    suspend fun load() {
        try {
            val resp = Api.notifications()
            // 服务端按 map 遍历返回，顺序不稳定，统一按时间倒序展示
            notifications = resp.items.sortedByDescending { it.created_at }
            unread = resp.unread
            val latestId = resp.items.maxOfOrNull { it.id } ?: 0L
            AppState.syncNotificationSummary(resp.unread, latestId)
        } catch (e: Exception) {
            Log.e(TAG, "load notifications failed: ${e.message}")
        }
        loading = false
        refreshing = false
    }

    LaunchedEffect(Unit) { load() }
    LaunchedEffect(messageRevision) {
        if (!loading && messageRevision > 0) load()
    }

    fun markAllRead() {
        if (unread == 0) return
        scope.launch {
            try {
                val resp = Api.markNotificationsRead(emptyList())
                unread = resp.unread
                notifications = notifications.map { it.copy(read = true) }
            } catch (err: Throwable) {
                context.toast(err.errorMessage("操作失败"))
            }
        }
    }

    fun openNotification(item: AppNotification) {
        var detailItem = item
        if (!item.read) {
            val previousUnread = unread
            val previousNotifications = notifications
            detailItem = item.copy(read = true)
            unread = maxOf(0, unread - 1)
            notifications = notifications.map { if (it.id == item.id) it.copy(read = true) else it }
            AppState.setUnreadCount(unread)
            scope.launch {
                try {
                    unread = Api.markNotificationsRead(listOf(item.id)).unread
                } catch (e: Exception) {
                    // 服务端失败时回滚未读状态
                    Log.e(TAG, "markNotificationRead failed: ${e.message}")
                    unread = previousUnread
                    notifications = previousNotifications
                    AppState.setUnreadCount(previousUnread)
                }
            }
        }
        nav.push(Route.NotificationDetail(detailItem))
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(
            title = "互动通知",
            rightText = if (unread > 0) "全部已读" else "",
            rightFontSize = 12,
            onBack = { nav.pop() },
            onRight = { markAllRead() },
        )

        if (loading) {
            LoadingView()
        } else {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    refreshing = true
                    scope.launch { load() }
                },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                ) {
                    // 键值带上已读态：否则已读后组件复用旧数据，卡片仍显示未读样式
                    items(notifications, key = { "${it.id}:${it.read}" }) { item ->
                        NotificationItemView(notification = item, onOpen = { openNotification(it) })
                    }
                    if (notifications.isEmpty()) {
                        item {
                            EmptyState(title = "暂无通知", desc = "点赞、评论等校园互动会出现在这里")
                        }
                    }
                }
            }
        }
    }
}

/** 通知详情（对齐鸿蒙端 pages/NotificationDetail.ets）。 */
@Composable
fun NotificationDetailScreen(nav: NavStack, notification: AppNotification) {
    fun typeLabel(): String = when (notification.type) {
        "like" -> "赞同通知"
        "comment" -> "评论通知"
        "reply" -> "回复通知"
        "report_result" -> "举报处理结果"
        "official_answer" -> "官方答复"
        "punishment" -> "账号处罚"
        "appeal_result" -> "申诉结果"
        else -> "系统通知"
    }

    val canOpenPost = notification.ref_type == "post" && notification.ref_id != null

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(title = "通知详情", onBack = { nav.pop() })

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.CARD_BG, RoundedCornerShape(16.dp))
                    .padding(20.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(AppColors.ICON_BG),
                    ) {
                        Text(
                            notificationTypeIcon(notification.type),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = notificationTypeColor(notification.type),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(typeLabel(), fontSize = 12.sp, color = notificationTypeColor(notification.type))
                        Text(
                            TimeUtil.format(notification.created_at),
                            fontSize = 11.sp,
                            color = AppColors.TEXT_SECONDARY,
                        )
                    }
                }

                HorizontalDivider(color = AppColors.DIVIDER)

                Text(
                    notification.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TEXT_PRIMARY,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    notification.body.ifEmpty { "暂无更多内容" },
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = AppColors.TEXT_SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (canOpenPost) {
                Button(
                    onClick = { nav.push(Route.PostDetail(notification.ref_id!!)) },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PRIMARY),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                ) {
                    Text("查看相关帖子", fontSize = 15.sp)
                }
            }
        }
    }
}

/** 私信聊天页（对齐鸿蒙端 pages/Chat.ets）：自由聊状态 + 打招呼握手解锁。 */
@Composable
fun ChatScreen(nav: NavStack, conversationId: Long, otherName: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 前台消息变更信号：NotificationSync 轮询发现新私信时递增，驱动本页原地刷新
    val messageRevision = AppState.messageRevision
    var messages by remember { mutableStateOf<List<DirectMessage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var unlocked by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var greeting by remember { mutableStateOf("你好，我想和你聊聊") }
    val listState = rememberLazyListState()

    // 返回消息页、切入其他页面时都强制补拉会话摘要
    DisposableEffect(Unit) {
        onDispose { AppState.requestMessageRefresh() }
    }

    suspend fun scrollToBottom() {
        delay(80)
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    /**
     * 收到全局消息变更信号后原地刷新当前会话，不用返回消息页再进入。
     * 自己发送 / 已读协议也会触发该信号，刷新是幂等的；仅在有新的对方未读消息时
     * 才调用已读协议，避免「已读 → 发布信号 → 再刷新」的自我循环。
     */
    suspend fun refreshConversation() {
        try {
            val previousLastId = messages.lastOrNull()?.id ?: 0L
            val resp = Api.getDirectConversation(conversationId)
            messages = resp.conversation.messages
            unlocked = resp.unlocked
            val currentLastId = messages.lastOrNull()?.id ?: 0L
            if (currentLastId != previousLastId) {
                scrollToBottom()
            }
            val myId = AppState.account?.id ?: -1L
            val hasUnreadIncoming = messages.any { it.sender_id != myId && it.status == "delivered" }
            if (hasUnreadIncoming) {
                // 正在会话内收到的对方消息立即已读：服务端持久化，底栏与消息页冒泡不回弹
                try {
                    Api.markDirectConversationRead(conversationId)
                } catch (e: Exception) {
                    Log.e(TAG, "refresh markConversationRead failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "refresh conversation failed: ${e.message}")
        }
    }

    LaunchedEffect(Unit) {
        try {
            val settings = Api.publicSettings()
            if (settings.greeting.isNotEmpty()) greeting = settings.greeting
        } catch (e: Exception) {
            Log.e(TAG, "load settings failed: ${e.message}")
        }
        try {
            val resp = Api.getDirectConversation(conversationId)
            messages = resp.conversation.messages
            unlocked = resp.unlocked
            scrollToBottom()
            // 进入会话即持久化已读
            try {
                Api.markDirectConversationRead(conversationId)
            } catch (e: Exception) {
                Log.e(TAG, "markConversationRead failed: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "load conversation failed: ${e.message}")
        }
        loading = false
    }

    LaunchedEffect(messageRevision) {
        if (!loading && messageRevision > 0) {
            refreshConversation()
        }
    }

    fun send(text: String, system: Boolean) {
        if (text.trim().isEmpty() || sending) return
        sending = true
        scope.launch {
            try {
                val resp = Api.sendDirectMessage(conversationId, text, system)
                messages = messages + resp.message
                unlocked = resp.unlocked
                draft = ""
                scrollToBottom()
            } catch (err: Throwable) {
                context.toast(err.errorMessage("发送失败，请稍后重试"))
            } finally {
                sending = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG).imePadding()) {
        NavBar(
            title = otherName.ifEmpty { "私信" },
            titleTag = if (unlocked) "自由聊" else "",
            onBack = { nav.pop() },
        )

        if (loading) {
            LoadingView()
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isMine = message.sender_id == (AppState.account?.id ?: -1L),
                    )
                }
                if (messages.isEmpty()) {
                    item {
                        EmptyState(title = "还没有消息", desc = "发出第一句话吧")
                    }
                }
            }

            if (unlocked) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("发送消息…", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY) },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = AppColors.CARD_BG,
                            focusedContainerColor = AppColors.CARD_BG,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = AppColors.PRIMARY,
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send,
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSend = { send(draft, false) },
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    )
                    Button(
                        onClick = { send(draft, false) },
                        enabled = draft.trim().isNotEmpty() && !sending,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.PRIMARY,
                            disabledContainerColor = AppColors.PRIMARY_DISABLED,
                        ),
                        modifier = Modifier.height(40.dp),
                    ) {
                        Text("发送", fontSize = 14.sp)
                    }
                }
            } else {
                // 未解锁：打招呼握手区，双方各回一条内置招呼后开放自由输入
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.CARD_BG)
                        .padding(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔒", fontSize = 16.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                            Text(
                                "回复后解锁自由聊天",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.TEXT_PRIMARY,
                            )
                            Text(
                                "首次联系双方各回复一条内置消息后，才可发送自由文字。",
                                fontSize = 12.sp,
                                color = AppColors.TEXT_SECONDARY,
                            )
                        }
                    }
                    Button(
                        onClick = { send(greeting, true) },
                        enabled = !sending,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PRIMARY),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                    ) {
                        Text("回复\"$greeting\"", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
