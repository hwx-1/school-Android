package com.example.schoolandorid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolandorid.config.AppColors
import com.example.schoolandorid.model.AIConversation
import com.example.schoolandorid.model.AIMessage
import com.example.schoolandorid.nav.NavStack
import com.example.schoolandorid.nav.Route
import com.example.schoolandorid.net.Api
import com.example.schoolandorid.ui.components.EmptyState
import com.example.schoolandorid.ui.components.LoadingView
import com.example.schoolandorid.ui.components.NavBar
import com.example.schoolandorid.ui.errorMessage
import com.example.schoolandorid.ui.toast
import com.example.schoolandorid.util.TimeUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** AI 问答页：会话列表 + 剩余次数 + 新建会话（对齐鸿蒙端 components/tabs/AITab.ets）。 */
@Composable
fun AIAssistantScreen(nav: NavStack) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var conversations by remember { mutableStateOf<List<AIConversation>>(emptyList()) }
    var remaining by remember { mutableIntStateOf(0) }

    suspend fun loadAll() {
        try {
            val resp = Api.aiConversations()
            conversations = resp.items
            remaining = resp.remaining
        } catch (_: Throwable) {
        }
        loading = false
        refreshing = false
    }

    LaunchedEffect(Unit) { loadAll() }

    fun createConversation() {
        scope.launch {
            try {
                val resp = Api.createAIConversation("", "")
                nav.push(Route.AIChat(resp.conversation.id, resp.conversation.title))
            } catch (err: Throwable) {
                context.toast(err.errorMessage("创建会话失败"))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(
            title = "AI 校园助手",
            subtitle = "今日剩余 $remaining 次提问",
            onBack = { nav.pop() },
            rightButtonText = "+ 新会话",
            onRightButton = { createConversation() },
        )

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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                ) {
                    // 键值带上标题与消息数：否则 AI 会话条目复用旧数据，消息条数不刷新
                    items(conversations, key = { "${it.id}:${it.title}:${it.messages.size}" }) { item ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.CARD_BG, RoundedCornerShape(12.dp))
                                .clickable { nav.push(Route.AIChat(item.id, item.title)) }
                                .padding(14.dp),
                        ) {
                            Text(
                                item.title.ifEmpty { "未命名会话" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.TEXT_PRIMARY,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("${item.messages.size} 条消息", fontSize = 12.sp, color = AppColors.TEXT_SECONDARY)
                                Spacer(Modifier.weight(1f))
                                Text(TimeUtil.format(item.created_at), fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
                            }
                        }
                    }
                    if (conversations.isEmpty()) {
                        item {
                            EmptyState(
                                title = "还没有 AI 会话",
                                desc = "试试问「图书馆几点关门」",
                                actionText = "开始提问",
                                onAction = { createConversation() },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * AI 对话页（对齐鸿蒙端 pages/AIChat.ets，仿 DeepSeek 移动端布局）：
 * 白底、AI 消息通栏无气泡、用户消息右侧浅灰气泡、底部圆角输入卡片。
 */
@Composable
fun AIChatScreen(nav: NavStack, conversationId: Long, conversationTitle: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<AIMessage>>(emptyList()) }
    var remaining by remember { mutableIntStateOf(-1) }
    var draft by remember { mutableStateOf("") }
    var asking by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(conversationTitle.ifEmpty { "AI 校园助手" }) }
    var deepThink by remember { mutableStateOf(false) }
    var webSearch by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val examples = listOf("图书馆几点关门？", "教务处电话是多少？", "奖学金怎么申请？")

    LaunchedEffect(Unit) {
        try {
            val resp = Api.aiConversations()
            remaining = resp.remaining
            val found = resp.items.find { it.id == conversationId }
            if (found != null) {
                messages = found.messages
                if (found.title.isNotEmpty()) title = found.title
            }
        } catch (_: Throwable) {
        }
    }

    suspend fun scrollToBottom() {
        delay(80)
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size + 1)
        }
    }

    fun ask(question: String? = null) {
        val q = (question ?: draft).trim()
        if (q.isEmpty() || asking) return
        asking = true
        draft = ""
        scope.launch {
            try {
                val resp = Api.askAI(conversationId, q, "")
                messages = messages + resp.user_message + resp.answer
                remaining = resp.remaining
                scrollToBottom()
            } catch (err: Throwable) {
                context.toast(err.errorMessage("提问失败，请稍后重试"))
            } finally {
                asking = false
            }
        }
    }

    fun newConversation() {
        scope.launch {
            try {
                val resp = Api.createAIConversation("", "")
                nav.replace(Route.AIChat(resp.conversation.id, resp.conversation.title))
            } catch (err: Throwable) {
                context.toast(err.errorMessage("创建会话失败"))
            }
        }
    }

    @Composable
    fun ToggleChip(label: String, active: Boolean, onTap: () -> Unit) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (active) AppColors.PRIMARY else AppColors.TEXT_SECONDARY,
            modifier = Modifier
                .background(
                    if (active) AppColors.PRIMARY_BG else Color.Transparent,
                    RoundedCornerShape(14.dp),
                )
                .border(
                    1.dp,
                    if (active) AppColors.PRIMARY else AppColors.DIVIDER,
                    RoundedCornerShape(14.dp),
                )
                .clickable { onTap() }
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).imePadding()) {
        NavBar(
            title = title,
            subtitle = if (remaining >= 0) "今日剩余 $remaining 次" else "",
            onBack = { nav.pop() },
            rightText = "✎",
            rightFontSize = 20,
            rightColor = AppColors.TEXT_PRIMARY,
            onRight = { newConversation() },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            if (messages.isEmpty()) {
                // 空会话欢迎页
                item(key = "welcome") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 64.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("✦", fontSize = 34.sp, color = AppColors.PRIMARY)
                            Text(
                                "有什么可以帮你？",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.TEXT_PRIMARY,
                            )
                            Text(
                                "我是 AI 校园助手，校内服务、办事流程都可以问我",
                                fontSize = 13.sp,
                                color = AppColors.TEXT_SECONDARY,
                            )
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            examples.forEach { example ->
                                Text(
                                    example,
                                    fontSize = 14.sp,
                                    color = AppColors.TEXT_PRIMARY,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AppColors.PAGE_BG, RoundedCornerShape(16.dp))
                                        .clickable { ask(example) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                if (message.role == "user") {
                    // 用户消息：右侧浅灰圆角气泡
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        Text(
                            message.text,
                            fontSize = 15.sp,
                            color = AppColors.TEXT_PRIMARY,
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .background(AppColors.BUBBLE_OTHER, RoundedCornerShape(18.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                } else {
                    // AI 消息：通栏无气泡，底部小字标注来源与甄别提示
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        Text(
                            message.text,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = AppColors.TEXT_PRIMARY,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!message.source.isNullOrEmpty()) {
                                Text(message.source, fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
                            }
                            Text("内容由 AI 生成，请仔细甄别", fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
                        }
                    }
                }
            }

            if (asking) {
                item(key = "thinking") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        CircularProgressIndicator(
                            color = AppColors.PRIMARY,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text("正在思考…", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY)
                    }
                }
            }
        }

        // 底部输入卡片
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(1.dp, AppColors.DIVIDER, RoundedCornerShape(24.dp))
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("有问题，尽管问", fontSize = 15.sp, color = AppColors.TEXT_SECONDARY) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp, max = 120.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ToggleChip("深度思考", deepThink) { deepThink = !deepThink }
                Spacer(Modifier.width(8.dp))
                ToggleChip("联网搜索", webSearch) { webSearch = !webSearch }
                Spacer(Modifier.weight(1f))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (draft.trim().isNotEmpty()) AppColors.PRIMARY else Color(0xFFC7CEDA),
                        )
                        .clickable(enabled = draft.trim().isNotEmpty() && !asking) { ask() },
                ) {
                    Text("↑", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
    }
}
