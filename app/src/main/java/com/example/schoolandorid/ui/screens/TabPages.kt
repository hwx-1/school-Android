package com.example.schoolandorid.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolandorid.config.AppColors
import com.example.schoolandorid.config.AppConfig
import com.example.schoolandorid.model.CampusTool
import com.example.schoolandorid.model.DirectConversationItem
import com.example.schoolandorid.nav.NavStack
import com.example.schoolandorid.nav.Route
import com.example.schoolandorid.net.Api
import com.example.schoolandorid.state.AppState
import com.example.schoolandorid.ui.components.AvatarView
import com.example.schoolandorid.ui.components.BadgeView
import com.example.schoolandorid.ui.components.ConversationItemView
import com.example.schoolandorid.ui.components.EmptyState
import com.example.schoolandorid.ui.components.LoadingView
import com.example.schoolandorid.ui.components.NavBar
import com.example.schoolandorid.ui.components.UnreadBadge
import com.example.schoolandorid.ui.toast
import kotlinx.coroutines.launch

private const val TAG = "TabPages"

/** 百宝箱 Tab：AI 助手入口横幅 + 全部校园工具（4 列网格）。 */
@Composable
fun ToolsTab(nav: NavStack) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<CampusTool>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }

    suspend fun load() {
        try {
            items = Api.listTools().filter { it.enabled }.sortedByDescending { it.weight }
        } catch (_: Throwable) {
        }
        loading = false
        refreshing = false
    }

    LaunchedEffect(Unit) { load() }

    fun openTool(tool: CampusTool) {
        if (tool.url.isNullOrEmpty()) {
            context.toast("该工具暂未开放链接")
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tool.url)))
        } catch (_: Exception) {
            context.toast("无法打开该链接")
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG),
    ) {
        NavBar(title = "百宝箱", showBack = false)

        // AI 助手入口横幅
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(AppColors.PRIMARY, AppColors.PRIMARY_LIGHT)))
                .clickable { nav.push(Route.AI) }
                .padding(16.dp),
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("AI 校园助手", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                Text("课表、图书馆、校历，问它就知道", fontSize = 11.sp, color = Color(0xCCFFFFFF))
            }
            Text("›", fontSize = 20.sp, color = Color.White)
        }

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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                ) {
                    items(items.chunked(4)) { rowTools ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowTools.forEach { tool ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(AppColors.CARD_BG, RoundedCornerShape(12.dp))
                                        .clickable { openTool(tool) }
                                        .padding(vertical = 16.dp, horizontal = 4.dp),
                                ) {
                                    Text(
                                        tool.icon.ifEmpty { if (tool.type == "link") "🔗" else "🧰" },
                                        fontSize = 26.sp,
                                    )
                                    Text(
                                        tool.name,
                                        fontSize = 12.sp,
                                        color = AppColors.TEXT_PRIMARY,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            repeat(4 - rowTools.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    if (items.isEmpty()) {
                        item { EmptyState(title = "暂无可用工具") }
                    }
                }
            }
        }
    }
}

/**
 * 消息页（抖音私信模式，对齐鸿蒙端 components/tabs/MessagesTab.ets）：
 * 顶部「互动通知」聚合入口卡片 + 私信会话列表；未读数与预览直接取服务端 unread_count。
 */
@Composable
fun MessagesTab(nav: NavStack) {
    val messageRevision = AppState.messageRevision
    val noticeUnread = AppState.unreadCount
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var conversations by remember { mutableStateOf<List<DirectConversationItem>>(emptyList()) }
    var latestNotice by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    /** 只允许最后一次刷新写入页面，避免旧请求晚返回覆盖新消息。 */
    var loadGeneration by remember { mutableStateOf(0) }

    suspend fun loadAll() {
        val generation = ++loadGeneration
        // 私信卡片优先刷新，避免通知接口延迟阻塞消息预览与未读状态
        try {
            val convResp = Api.listDirectConversations()
            if (generation == loadGeneration) {
                conversations = convResp.items
                AppState.syncDirectConversations(convResp.items, convResp.unread)
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadAll conversations failed: ${e.message}")
        }
        try {
            val resp = Api.notifications()
            if (generation == loadGeneration) {
                val latest = resp.items.maxByOrNull { it.created_at }
                latestNotice = latest?.title ?: ""
                val latestId = resp.items.maxOfOrNull { it.id } ?: 0L
                AppState.syncNotificationSummary(resp.unread, latestId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadAll notifications failed: ${e.message}")
        }
        if (generation == loadGeneration) {
            loading = false
            refreshing = false
        }
    }

    LaunchedEffect(Unit) { loadAll() }
    LaunchedEffect(messageRevision) {
        if (!loading && messageRevision > 0) {
            // 后台同步或发送响应已带回完整快照时先同步更新卡片，再做网络校准
            if (AppState.hasDirectSnapshot()) {
                conversations = AppState.directConversationSnapshot()
            }
            loadAll()
        }
    }

    /** 进入会话后本地立即把对方消息置为已读，未读数冒泡即时消失。 */
    fun markConversationMessagesRead(conversationId: Long) {
        val myId = AppState.account?.id ?: -1L
        conversations = conversations.map { conversation ->
            if (conversation.id != conversationId) return@map conversation
            conversation.copy(
                unread_count = 0,
                messages = conversation.messages.map { message ->
                    if (message.sender_id == myId || message.status != "delivered") message
                    else message.copy(status = "read")
                },
            )
        }
        // 底栏冒泡立即更新，不等待下次拉取
        AppState.setDirectUnreadCount(conversations.sumOf { it.unread_count })
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(title = "消息", showBack = false)

        if (loading) {
            LoadingView()
        } else {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    refreshing = true
                    scope.launch { loadAll() }
                },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                ) {
                    // 互动通知聚合入口卡片
                    item(key = "notice_entry") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.CARD_BG, RoundedCornerShape(12.dp))
                                .clickable { nav.push(Route.Notifications) }
                                .padding(14.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.PRIMARY_BG),
                            ) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = AppColors.PRIMARY,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    "互动通知",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.TEXT_PRIMARY,
                                )
                                Text(
                                    latestNotice.ifEmpty { "点赞、评论等互动会出现在这里" },
                                    fontSize = 13.sp,
                                    color = AppColors.TEXT_SECONDARY,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (noticeUnread > 0) {
                                UnreadBadge(count = noticeUnread)
                            }
                        }
                    }

                    if (conversations.isNotEmpty()) {
                        item(key = "dm_label") {
                            Text(
                                "私信",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.TEXT_SECONDARY,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
                            )
                        }
                        items(
                            conversations,
                            // 键值必须包含驱动卡片渲染的字段：仅用会话 id 时复用旧组件，
                            // 未读数/预览/时间会冻结在首次构建的状态
                            key = { "${it.id}:${it.unread_count}:${it.updated_at}:${it.messages.size}" },
                        ) { item ->
                            ConversationItemView(
                                conversation = item,
                                onOpen = { target ->
                                    markConversationMessagesRead(target.id)
                                    nav.push(Route.Chat(target.id, target.other.nickname))
                                },
                            )
                        }
                    }

                    if (conversations.isEmpty() && latestNotice.isEmpty()) {
                        item(key = "empty") {
                            EmptyState(title = "暂无消息", desc = "新的校园互动会出现在这里")
                        }
                    }
                }
            }
        }
    }
}

/** 我的：资料卡 + 功能入口 + 退出登录（对齐鸿蒙端 components/tabs/MineTab.ets）。 */
@Composable
fun MineTab(nav: NavStack) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = AppState.account
    var showLogoutDialog by remember { mutableStateOf(false) }

    fun profileCompleteness(): Int {
        val acc = account ?: return 0
        var done = 0
        if (acc.avatar.isNotEmpty()) done++
        if (!acc.real_name.isNullOrEmpty()) done++
        if (!acc.student_no.isNullOrEmpty()) done++
        if (!acc.class_name.isNullOrEmpty()) done++
        if (acc.gender.isNotEmpty()) done++
        return (done * 100) / 5
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    scope.launch {
                        AppState.logout()
                        context.toast("已退出登录")
                        nav.replace(Route.Login)
                    }
                }) {
                    Text("退出", color = AppColors.DANGER)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消", color = AppColors.TEXT_SECONDARY)
                }
            },
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.PAGE_BG)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // 资料卡
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.CARD_BG, RoundedCornerShape(14.dp))
                .clickable { nav.push(Route.EditProfile) }
                .padding(16.dp),
        ) {
            AvatarView(
                avatar = account?.avatar ?: "",
                nickname = account?.nickname ?: "",
                diameter = 60,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        account?.nickname ?: "未登录",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TEXT_PRIMARY,
                    )
                    if (account?.verified == true) {
                        BadgeView(badge = account?.badge ?: "org")
                    }
                }
                Text(account?.phone ?: "", fontSize = 13.sp, color = AppColors.TEXT_SECONDARY)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("资料完整度 ${profileCompleteness()}%", fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
                    LinearProgressIndicator(
                        progress = { profileCompleteness() / 100f },
                        color = AppColors.PRIMARY,
                        trackColor = AppColors.DIVIDER,
                        modifier = Modifier.width(100.dp),
                    )
                }
            }
        }

        // 功能入口
        Column(modifier = Modifier.fillMaxWidth().background(AppColors.CARD_BG, RoundedCornerShape(14.dp))) {
            EntryRow(Icons.Outlined.Description, "我的帖子", "") { nav.push(Route.ContentList("mine")) }
            EntryRow(Icons.Outlined.Star, "我的收藏", "") { nav.push(Route.ContentList("bookmarks")) }
            EntryRow(
                Icons.Outlined.School,
                "学生认证",
                if (account?.verified == true) "已认证" else "未认证",
            ) { nav.push(Route.Verification) }
        }

        Column(modifier = Modifier.fillMaxWidth().background(AppColors.CARD_BG, RoundedCornerShape(14.dp))) {
            EntryRow(Icons.Outlined.Notifications, "消息通知设置", "") { nav.push(Route.NotificationSettings) }
            EntryRow(Icons.Outlined.Settings, "账号与安全", "") { nav.push(Route.AccountSettings) }
        }

        Column(modifier = Modifier.fillMaxWidth().background(AppColors.CARD_BG, RoundedCornerShape(14.dp))) {
            EntryRow(Icons.Outlined.Description, "隐私政策", "") { nav.push(Route.LegalDocument("privacy")) }
            EntryRow(Icons.Outlined.Description, "用户协议", "") { nav.push(Route.LegalDocument("agreement")) }
            EntryRow(Icons.Outlined.Person, "关于沈大社区", "v${AppConfig.APP_VERSION}") { nav.push(Route.About) }
        }

        Text(
            "退出登录",
            fontSize = 15.sp,
            color = AppColors.DANGER,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.CARD_BG, RoundedCornerShape(14.dp))
                .clickable { showLogoutDialog = true }
                .padding(14.dp),
        )
    }
}

@Composable
private fun EntryRow(icon: ImageVector, label: String, detail: String, action: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { action() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.TEXT_SECONDARY, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 15.sp, color = AppColors.TEXT_PRIMARY, modifier = Modifier.weight(1f))
        if (detail.isNotEmpty()) {
            Text(detail, fontSize = 12.sp, color = AppColors.TEXT_SECONDARY)
        }
        Text("›", fontSize = 18.sp, color = AppColors.TEXT_SECONDARY)
    }
}
