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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import com.example.schoolandorid.config.AppColors
import com.example.schoolandorid.model.AIConversation
import com.example.schoolandorid.model.AIMessage
import com.example.schoolandorid.nav.NavStack
import com.example.schoolandorid.nav.Route
import com.example.schoolandorid.net.Api
import com.example.schoolandorid.ui.components.EmptyState
import com.example.schoolandorid.ui.errorMessage
import com.example.schoolandorid.ui.toast
import com.example.schoolandorid.util.TimeUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AI 对话页（对齐鸿蒙端 pages/AIChat.ets，仿 DeepSeek 移动端布局）：
 * 进入即展开新对话，右上角提供「对话记录」与「新对话」两个入口，历史会话以内嵌弹层展示。
 * 发送后立即展示用户消息，AI 回复走 SSE 流式渲染，思考过程可收起/展开（默认收起）。
 * conversationId 为 null 时自动创建新会话；否则加载对应会话。
 */
@Composable
fun AIChatScreen(nav: NavStack, conversationId: Long?, conversationTitle: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<AIMessage>>(emptyList()) }
    var remaining by remember { mutableIntStateOf(-1) }
    var draft by remember { mutableStateOf("") }
    var asking by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(conversationTitle.ifEmpty { "AI 校园助手" }) }
    var conversations by remember { mutableStateOf<List<AIConversation>>(emptyList()) }
    var showHistory by remember { mutableStateOf(false) }
    var deepThink by remember { mutableStateOf(false) }
    var webSearch by remember { mutableStateOf(false) }
    var streamReasoning by remember { mutableStateOf("") }
    var streamText by remember { mutableStateOf("") }
    var reasoningExpanded by remember { mutableStateOf(false) }
    var pendingUserTempId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val examples = listOf("图书馆几点关门？", "教务处电话是多少？", "奖学金怎么申请？")

    suspend fun loadConversations() {
        try {
            val resp = Api.aiConversations()
            remaining = resp.remaining
            // 历史记录按时间倒序：最新在上，越久越靠下
            conversations = resp.items.sortedByDescending { it.created_at }
        } catch (_: Throwable) {
        }
    }

    suspend fun loadCurrent() {
        loadConversations()
        val id = conversationId
        if (id != null) {
            val found = conversations.find { it.id == id }
            messages = found?.messages ?: emptyList()
            if (found != null && found.title.isNotEmpty()) title = found.title
        }
    }

    LaunchedEffect(Unit) {
        if (conversationId == null) {
            // 直接进入 AI 页时自动创建新会话并切换到带 id 的路由
            try {
                val resp = Api.createAIConversation("", "")
                nav.replace(Route.AIChat(resp.conversation.id, resp.conversation.title))
            } catch (err: Throwable) {
                context.toast(err.errorMessage("创建会话失败"))
                nav.pop()
            }
            return@LaunchedEffect
        }
        loadCurrent()
    }

    suspend fun scrollToBottom() {
        delay(60)
        val count = if (messages.isEmpty()) 1 else messages.size + if (asking) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    LaunchedEffect(streamText, streamReasoning) {
        if (asking) scrollToBottom()
    }

    fun ask(question: String? = null) {
        val id = conversationId ?: return
        val q = (question ?: draft).trim()
        if (q.isEmpty() || asking) return
        asking = true
        draft = ""
        val tempId = -(System.currentTimeMillis())
        pendingUserTempId = tempId
        // 立即展示用户消息，不等 AI 回答
        messages = messages + AIMessage(id = tempId, role = "user", text = q, created_at = "")
        streamReasoning = ""
        streamText = ""
        reasoningExpanded = false
        scope.launch {
            try {
                Api.askAIStream(id, q, "") { event ->
                    when (event.type) {
                        "thinking" -> streamReasoning += event.delta.orEmpty()
                        "text" -> streamText += event.delta.orEmpty()
                        "done" -> {
                            val userMsg = event.user_message
                            val answerMsg = event.answer
                            val base = messages.filterNot { it.id == pendingUserTempId }
                            val user = userMsg ?: AIMessage(id = tempId, role = "user", text = q, created_at = "")
                            val answer = answerMsg ?: AIMessage(
                                id = tempId - 1,
                                role = "assistant",
                                text = streamText,
                                reasoning = streamReasoning.ifBlank { null },
                                created_at = "",
                            )
                            messages = base + listOf(user, answer)
                            remaining = event.remaining
                            asking = false
                            streamReasoning = ""
                            streamText = ""
                            scope.launch { scrollToBottom() }
                        }
                        "error" -> {
                            context.toast(event.message ?: "回答中断，本次不扣额度")
                            asking = false
                            streamReasoning = ""
                            streamText = ""
                        }
                    }
                }
            } catch (err: Throwable) {
                context.toast(err.errorMessage("提问失败，请稍后重试"))
                asking = false
                streamReasoning = ""
                streamText = ""
            }
        }
    }

    fun newConversation() {
        if (asking) return
        scope.launch {
            try {
                val resp = Api.createAIConversation("", "")
                nav.replace(Route.AIChat(resp.conversation.id, resp.conversation.title))
            } catch (err: Throwable) {
                context.toast(err.errorMessage("创建会话失败"))
            }
        }
    }

    fun openConversation(item: AIConversation) {
        nav.replace(Route.AIChat(item.id, item.title))
    }

    fun showHistoryPanel() {
        showHistory = true
        scope.launch { loadConversations() }
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

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            AIChatTopBar(
                title = title,
                subtitle = if (remaining >= 0) "今日剩余 $remaining 次" else "",
                onBack = { nav.pop() },
                onHistory = { showHistoryPanel() },
                onNew = { newConversation() },
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                if (messages.isEmpty() && !asking) {
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
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
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
                        AssistantMessageBubble(message)
                    }
                }

                if (asking) {
                    item(key = "streaming") {
                        AssistantBubble(
                            reasoning = streamReasoning,
                            text = streamText,
                            source = null,
                            expanded = reasoningExpanded,
                            onToggle = { reasoningExpanded = !reasoningExpanded },
                            showSpinner = streamReasoning.isEmpty() && streamText.isEmpty(),
                        )
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

        if (showHistory) {
            HistoryDialog(
                conversations = conversations,
                currentId = conversationId,
                onSelect = { openConversation(it) },
                onClose = { showHistory = false },
            )
        }
    }
}

/** 持久化的 AI 消息气泡（含可收起的思考过程）。 */
@Composable
private fun AssistantMessageBubble(message: AIMessage) {
    var expanded by remember(message.id) { mutableStateOf(false) }
    AssistantBubble(
        reasoning = message.reasoning.orEmpty(),
        text = message.text,
        source = message.source,
        expanded = expanded,
        onToggle = { expanded = !expanded },
    )
}

/** AI 回答气泡：思考过程可收起（默认收起），正文在思考过程下方。 */
@Composable
private fun AssistantBubble(
    reasoning: String,
    text: String,
    source: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    showSpinner: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.CARD_BG, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.DIVIDER, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        if (reasoning.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
            ) {
                Text("思考过程", fontSize = 12.sp, color = AppColors.TEXT_SECONDARY)
                Spacer(Modifier.weight(1f))
                Text(
                    if (expanded) "收起" else "展开",
                    fontSize = 12.sp,
                    color = AppColors.PRIMARY,
                )
            }
            if (expanded) {
                Text(
                    reasoning,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = AppColors.TEXT_SECONDARY,
                )
            }
        }
        if (text.isNotEmpty()) {
            Text(
                text,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = AppColors.TEXT_PRIMARY,
            )
        }
        if (showSpinner) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    color = AppColors.PRIMARY,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Text("正在思考…", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY)
            }
        }
        if (!source.isNullOrEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(source, fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
                Text("内容由 AI 生成，请仔细甄别", fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
            }
        }
    }
}

/** AI 对话页顶部栏：返回 + 居中标题 + 右上角「对话记录 / 新对话」两个入口。 */
@Composable
private fun AIChatTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onHistory: () -> Unit,
    onNew: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 108.dp),
        ) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TEXT_PRIMARY,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = AppColors.TEXT_SECONDARY,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                "‹",
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TEXT_PRIMARY,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Text(
                "对话记录",
                fontSize = 13.sp,
                color = AppColors.TEXT_SECONDARY,
                modifier = Modifier
                    .clickable { onHistory() }
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            )
            Text(
                "新对话",
                fontSize = 13.sp,
                color = AppColors.PRIMARY,
                modifier = Modifier
                    .clickable { onNew() }
                    .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}

/** 对话记录弹层：展示历史会话，点击切换会话。 */
@Composable
private fun HistoryDialog(
    conversations: List<AIConversation>,
    currentId: Long?,
    onSelect: (AIConversation) -> Unit,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AppColors.PAGE_BG,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "对话记录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TEXT_PRIMARY,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "✕",
                        fontSize = 18.sp,
                        color = AppColors.TEXT_SECONDARY,
                        modifier = Modifier
                            .clickable { onClose() }
                            .padding(8.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (conversations.isEmpty()) {
                    EmptyState(title = "还没有 AI 会话", desc = "开始提问后会自动保存")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(conversations, key = { it.id }) { item ->
                            val selected = item.id == currentId
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selected) AppColors.PRIMARY_BG else AppColors.CARD_BG,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable { onSelect(item) }
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
                    }
                }
            }
        }
    }
}
