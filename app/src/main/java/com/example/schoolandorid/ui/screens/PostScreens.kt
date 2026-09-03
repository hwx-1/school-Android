package com.example.schoolandorid.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.schoolandorid.config.AppColors
import com.example.schoolandorid.config.AppConfig
import com.example.schoolandorid.model.Announcement
import com.example.schoolandorid.model.CommentItem
import com.example.schoolandorid.model.Post
import com.example.schoolandorid.nav.NavStack
import com.example.schoolandorid.nav.Route
import com.example.schoolandorid.net.Api
import com.example.schoolandorid.net.Http
import com.example.schoolandorid.state.AppState
import com.example.schoolandorid.ui.components.AnnouncementCard
import com.example.schoolandorid.ui.components.AvatarView
import com.example.schoolandorid.ui.components.BadgeView
import com.example.schoolandorid.ui.components.CommentItemView
import com.example.schoolandorid.ui.components.EmptyState
import com.example.schoolandorid.ui.components.LoadingView
import com.example.schoolandorid.ui.components.NavBar
import com.example.schoolandorid.ui.components.PostCard
import com.example.schoolandorid.ui.components.ReportDialog
import com.example.schoolandorid.ui.components.ReportTarget
import com.example.schoolandorid.ui.components.TagChip
import com.example.schoolandorid.ui.errorMessage
import com.example.schoolandorid.ui.toast
import com.example.schoolandorid.util.ImageUtils
import com.example.schoolandorid.util.TimeUtil
import kotlinx.coroutines.launch

/** 帖子详情（对齐鸿蒙端 pages/PostDetail.ets）：抖音式两级评论 + 底部评论输入。 */
@Composable
fun PostDetailScreen(nav: NavStack, postId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val commentFocusRequester = remember { FocusRequester() }
    val postRevision = AppState.postRevision
    var post by remember { mutableStateOf<Post?>(null) }
    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf("") }
    var commentText by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var startingChat by remember { mutableStateOf(false) }
    var replyTo by remember { mutableStateOf<CommentItem?>(null) }
    var expandedRoots by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var menuOpen by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<ReportTarget?>(null) }
    var deleteConfirm by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    suspend fun loadPost(): Boolean {
        return try {
            post = Api.getPost(postId).post
            true
        } catch (err: Throwable) {
            loadError = err.errorMessage("帖子加载失败")
            false
        }
    }

    suspend fun loadAll() {
        loadError = ""
        if (!loadPost()) {
            loading = false
            return
        }
        try {
            comments = Api.listComments(postId).items
        } catch (_: Throwable) {
        }
        loading = false
    }

    LaunchedEffect(Unit) { loadAll() }
    LaunchedEffect(postRevision) {
        if (!loading && postRevision > 0) loadPost()
    }

    // ---- 抖音式两级评论 ----
    fun commentById(id: Long): CommentItem? = comments.find { it.id == id }

    fun rootIdOf(comment: CommentItem): Long {
        var current = comment
        val seen = mutableSetOf<Long>()
        while ((current.parent_id ?: 0L) > 0 && current.id !in seen) {
            seen.add(current.id)
            val parent = commentById(current.parent_id!!) ?: break
            current = parent
        }
        return current.id
    }

    fun rootComments(): List<CommentItem> = comments.filter {
        val pid = it.parent_id
        pid == null || pid == 0L || commentById(pid) == null
    }

    fun repliesOf(rootId: Long): List<CommentItem> = comments.filter {
        val pid = it.parent_id
        pid != null && pid > 0 && rootIdOf(it) == rootId
    }

    fun visibleReplies(rootId: Long): List<CommentItem> {
        val replies = repliesOf(rootId)
        return if (rootId in expandedRoots) replies else replies.take(2)
    }

    fun replyTargetName(comment: CommentItem): String {
        val pid = comment.parent_id ?: return ""
        if (pid == 0L || pid == rootIdOf(comment)) return ""
        val parent = commentById(pid) ?: return ""
        if (parent.deleted) return ""
        return "@${parent.author.nickname}"
    }

    fun startReply(comment: CommentItem) {
        replyTo = comment
    }

    /** 删除帖子（对齐鸿蒙端 PostDetail.ets confirmDeletePost）：确认后删除并返回。 */
    fun deletePost() {
        val p = post ?: return
        scope.launch {
            try {
                Api.deletePost(p.id)
                context.toast("帖子已删除")
                nav.pop()
            } catch (err: Throwable) {
                context.toast(err.errorMessage("删除失败"))
            }
        }
    }

    /** 复制帖子链接（对齐鸿蒙端 PostDetail.ets copyLink）。 */
    fun copyLink() {
        val p = post ?: return
        clipboard.setText(AnnotatedString("${AppConfig.API_BASE_URL}/posts/${p.id}"))
        context.toast("帖子链接已复制")
    }

    fun startChat() {
        val p = post ?: return
        if (startingChat) return
        startingChat = true
        scope.launch {
            try {
                val resp = Api.startDirectConversation(p.author.id)
                nav.push(Route.Chat(resp.item.id, p.author.nickname))
            } catch (err: Throwable) {
                context.toast(err.errorMessage("发起私信失败，请稍后重试"))
            } finally {
                startingChat = false
            }
        }
    }

    fun handleLike() {
        val p = post ?: return
        scope.launch {
            try {
                post = Api.likePost(p.id).post
            } catch (err: Throwable) {
                context.toast(err.errorMessage("操作失败"))
            }
        }
    }

    fun handleBookmark() {
        val p = post ?: return
        scope.launch {
            try {
                val resp = Api.bookmarkPost(p.id)
                post = resp.post
                context.toast(if (resp.post.bookmarked) "已收藏" else "已取消收藏")
            } catch (err: Throwable) {
                context.toast(err.errorMessage("操作失败"))
            }
        }
    }

    fun sendComment() {
        val text = commentText.trim()
        if (text.isEmpty() || sending) return
        sending = true
        scope.launch {
            try {
                val resp = Api.createComment(postId, text, replyTo?.id)
                if (!resp.moderation.pass) {
                    context.toast(resp.message.ifEmpty { "评论未通过内容审核" })
                } else {
                    comments = comments + resp.comment
                    commentText = ""
                    replyTo = null
                    focusManager.clearFocus()
                    post?.let { post = it.copy(comments = it.comments + 1) }
                }
            } catch (err: Throwable) {
                context.toast(err.errorMessage("评论失败，请稍后重试"))
            } finally {
                sending = false
            }
        }
    }

    LaunchedEffect(replyTo) {
        if (replyTo != null) {
            kotlinx.coroutines.delay(50)
            commentFocusRequester.requestFocus()
        }
    }

    // 举报弹窗（对齐 web 端 PostDetailPage ReportDialog）
    reportTarget?.let { target ->
        ReportDialog(target = target, onClose = { reportTarget = null })
    }

    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text("删除帖子") },
            text = { Text("删除后不可恢复，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirm = false
                    deletePost()
                }) {
                    Text("删除", color = AppColors.DANGER)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = false }) {
                    Text("取消", color = AppColors.TEXT_SECONDARY)
                }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG).imePadding()) {
        NavBar(title = "帖子详情", onBack = { nav.pop() })

        when {
            loading -> LoadingView()
            loadError.isNotEmpty() || post == null -> EmptyState(
                title = "加载失败",
                desc = loadError,
                actionText = "重试",
                onAction = {
                    scope.launch {
                        loading = true
                        loadAll()
                    }
                },
            )

            else -> {
                val p = post!!
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    // 正文区
                    item(key = "post_body") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.CARD_BG, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val isOwnPost = p.author.id == (AppState.account?.id ?: -1L)
                                Box(modifier = Modifier.clickable { nav.push(Route.UserProfile(p.author.id)) }) {
                                    AvatarView(p.author.avatar, p.author.nickname, 44)
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { nav.push(Route.UserProfile(p.author.id)) },
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            p.author.nickname,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AppColors.TEXT_PRIMARY,
                                        )
                                        if (p.author.verified) {
                                            BadgeView(badge = p.author.badge ?: "org")
                                        }
                                    }
                                    Text(TimeUtil.format(p.created_at), fontSize = 12.sp, color = AppColors.TEXT_SECONDARY)
                                }
                                if (!isOwnPost) {
                                    Button(
                                        onClick = { startChat() },
                                        enabled = !startingChat,
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PRIMARY),
                                    ) {
                                        Text("私信", fontSize = 13.sp)
                                    }
                                }
                                // 更多操作（对齐 web 端 PostCard 菜单 / 鸿蒙端 PostDetail 更多菜单）
                                Box {
                                    Text(
                                        "⋯",
                                        fontSize = 20.sp,
                                        color = AppColors.TEXT_SECONDARY,
                                        modifier = Modifier
                                            .clickable { menuOpen = true }
                                            .padding(horizontal = 6.dp),
                                    )
                                    DropdownMenu(
                                        expanded = menuOpen,
                                        onDismissRequest = { menuOpen = false },
                                    ) {
                                        if (isOwnPost) {
                                            DropdownMenuItem(
                                                text = { Text("编辑帖子") },
                                                onClick = {
                                                    menuOpen = false
                                                    nav.push(Route.Compose(p.id))
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("删除帖子", color = AppColors.DANGER) },
                                                onClick = {
                                                    menuOpen = false
                                                    deleteConfirm = true
                                                },
                                            )
                                        } else {
                                            DropdownMenuItem(
                                                text = { Text("举报帖子") },
                                                onClick = {
                                                    menuOpen = false
                                                    reportTarget = ReportTarget("post", p.id)
                                                },
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("复制链接") },
                                            onClick = {
                                                menuOpen = false
                                                copyLink()
                                            },
                                        )
                                    }
                                }
                            }

                            Text(p.text, fontSize = 16.sp, color = AppColors.TEXT_PRIMARY, modifier = Modifier.fillMaxWidth())

                            if (!p.images.isNullOrEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    p.images.forEach { path ->
                                        AsyncImage(
                                            model = Http.absoluteMediaUrl(path),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(AppColors.ICON_BG),
                                        )
                                    }
                                }
                            }

                            if (!p.tags.isNullOrEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    p.tags.forEach { tag ->
                                        TagChip(text = tag, onTap = { nav.push(Route.Search(it)) })
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { handleLike() }
                                        .padding(8.dp),
                                ) {
                                    Icon(
                                        if (p.liked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "赞同",
                                        tint = if (p.liked) AppColors.PRIMARY else AppColors.TEXT_SECONDARY,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "${p.likes}",
                                        fontSize = 13.sp,
                                        color = if (p.liked) AppColors.PRIMARY else AppColors.TEXT_SECONDARY,
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { handleBookmark() }
                                        .padding(8.dp),
                                ) {
                                    Icon(
                                        if (p.bookmarked) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "收藏",
                                        tint = if (p.bookmarked) AppColors.ORANGE else AppColors.TEXT_SECONDARY,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "${p.bookmarks}",
                                        fontSize = 13.sp,
                                        color = if (p.bookmarked) AppColors.ORANGE else AppColors.TEXT_SECONDARY,
                                    )
                                }
                            }
                        }
                    }

                    // 评论区
                    item(key = "comments") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.CARD_BG, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp),
                        ) {
                            Text(
                                "评论 ${comments.size}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.TEXT_PRIMARY,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                            )
                            if (comments.isEmpty()) {
                                EmptyState(title = "还没有评论", desc = "抢个沙发吧")
                            } else {
                                rootComments().forEach { root ->
                                    Column {
                                        CommentItemView(
                                            comment = root,
                                            onReply = { startReply(it) },
                                            onReport = { reportTarget = ReportTarget("comment", it.id) },
                                            onAuthor = { nav.push(Route.UserProfile(it.author.id)) },
                                        )
                                        val replies = repliesOf(root.id)
                                        if (replies.isNotEmpty()) {
                                            Column(modifier = Modifier.padding(start = 42.dp)) {
                                                visibleReplies(root.id).forEach { reply ->
                                                    CommentItemView(
                                                        comment = reply,
                                                        isReply = true,
                                                        replyToName = replyTargetName(reply),
                                                        onReply = { startReply(it) },
                                                        onReport = { reportTarget = ReportTarget("comment", it.id) },
                                                        onAuthor = { nav.push(Route.UserProfile(it.author.id)) },
                                                    )
                                                }
                                                if (replies.size > 2) {
                                                    Text(
                                                        if (root.id in expandedRoots) "收起回复 ∧"
                                                        else "展开 ${replies.size - 2} 条回复 ⌄",
                                                        fontSize = 12.sp,
                                                        color = AppColors.TEXT_SECONDARY,
                                                        modifier = Modifier
                                                            .clickable {
                                                                expandedRoots = if (root.id in expandedRoots) {
                                                                    expandedRoots - root.id
                                                                } else {
                                                                    expandedRoots + root.id
                                                                }
                                                            }
                                                            .padding(top = 2.dp, bottom = 6.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    item(key = "bottom_spacer") { Spacer(Modifier.height(12.dp)) }
                }

                // 底部评论输入
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.CARD_BG)
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    if (replyTo != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
                        ) {
                            Text(
                                "回复 @${replyTo!!.author.nickname}：",
                                fontSize = 12.sp,
                                color = AppColors.TEXT_SECONDARY,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "×",
                                fontSize = 16.sp,
                                color = AppColors.TEXT_SECONDARY,
                                modifier = Modifier.clickable {
                                    replyTo = null
                                    focusManager.clearFocus()
                                },
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = {
                                Text(
                                    if (replyTo != null) "回复 @${replyTo!!.author.nickname}：" else "友善评论，温暖校园",
                                    fontSize = 14.sp,
                                    color = AppColors.TEXT_SECONDARY,
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = AppColors.PAGE_BG,
                                focusedContainerColor = AppColors.PAGE_BG,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = AppColors.PRIMARY,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .focusRequester(commentFocusRequester),
                        )
                        Button(
                            onClick = { sendComment() },
                            enabled = commentText.trim().isNotEmpty() && !sending,
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
                }
            }
        }
    }
}

/** 待上传图片：本地缓存路径 + 可选的远端 URL（上传完成后回填）。 */
private data class PendingImage(
    val localPath: String,
    val remoteUrl: String,
    val uploading: Boolean,
)

/** 排序选项（与首页一致） */
private val searchSortOptions = listOf(
    "time" to "最新",
    "comments" to "评论",
    "likes" to "点赞",
    "bookmarks" to "收藏",
)

/** 发布 / 编辑动态（对齐鸿蒙端 pages/Compose.ets 与 web 端 /compose/:postId）。 */
@Composable
fun ComposeScreen(nav: NavStack, postId: Long = 0) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditing = postId > 0
    var text by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var images by remember { mutableStateOf<List<PendingImage>>(emptyList()) }
    var publishing by remember { mutableStateOf(false) }
    var loadingPost by remember { mutableStateOf(isEditing) }

    // 编辑模式：载入旧帖内容（新版本审核通过前旧版本继续公开）
    LaunchedEffect(postId) {
        if (!isEditing) return@LaunchedEffect
        try {
            val post = Api.getPost(postId).post
            text = post.text
            tags = post.tags.orEmpty()
            images = post.images.orEmpty().map { PendingImage(localPath = "", remoteUrl = it, uploading = false) }
        } catch (err: Throwable) {
            context.toast(err.errorMessage("帖子不存在或已删除"))
            nav.pop()
        }
        loadingPost = false
    }

    fun canPublish() = text.trim().isNotEmpty() && !publishing && !loadingPost && images.none { it.uploading }

    fun importImage(uri: android.net.Uri) {
        scope.launch {
            val fileName = "post_${System.currentTimeMillis()}_${(0..99999).random()}.jpg"
            val cacheFile = ImageUtils.copyToCache(context, uri, fileName)
            if (cacheFile == null) {
                context.toast("图片读取失败，请重试")
                return@launch
            }
            val pending = PendingImage(localPath = cacheFile.absolutePath, remoteUrl = "", uploading = true)
            images = images + pending
            try {
                val resp = Http.uploadFile(cacheFile, fileName, "image/jpeg")
                images = images.map {
                    if (it.localPath == pending.localPath) it.copy(remoteUrl = resp.url, uploading = false) else it
                }
            } catch (err: Throwable) {
                context.toast(err.errorMessage("图片上传失败"))
                images = images.filter { it.localPath != pending.localPath }
            }
        }
    }

    // PickMultipleVisualMedia 要求 maxItems > 1，不能把“剩余可传张数”（可能为 1）直接作为参数；
    // 固定为上限，实际数量在回调里按剩余可传张数截断，避免选多张时闪退。
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(AppConfig.MAX_POST_IMAGES),
    ) { uris ->
        val remain = (AppConfig.MAX_POST_IMAGES - images.size).coerceAtLeast(0)
        uris?.take(remain)?.forEach { importImage(it) }
    }

    fun pickImagesClick() {
        val remain = AppConfig.MAX_POST_IMAGES - images.size
        if (remain <= 0) {
            context.toast("最多上传 ${AppConfig.MAX_POST_IMAGES} 张图片")
            return
        }
        pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun addTag() {
        val tag = tagInput.trim().removePrefix("#")
        if (tag.isEmpty()) return
        if (tag in tags) {
            context.toast("标签已存在")
            return
        }
        if (tags.size >= AppConfig.MAX_POST_TAGS) {
            context.toast("最多 ${AppConfig.MAX_POST_TAGS} 个标签")
            return
        }
        tags = tags + tag
        tagInput = ""
    }

    fun publish() {
        if (!canPublish()) return
        publishing = true
        scope.launch {
            try {
                if (isEditing) {
                    val resp = Api.updatePost(
                        id = postId,
                        text = text.trim(),
                        images = images.map { it.remoteUrl },
                        tags = tags,
                    )
                    context.toast(resp.message.ifEmpty { "已保存，审核通过后展示新版本" })
                    nav.pop()
                } else {
                    val resp = Api.createPost(
                        text = text.trim(),
                        images = images.map { it.remoteUrl },
                        tags = tags,
                    )
                    if (!resp.moderation.pass) {
                        context.toast(resp.message.ifEmpty { "内容未通过审核，请修改后重试" })
                    } else {
                        context.toast(resp.message.ifEmpty { "发布成功" })
                        nav.pop()
                    }
                }
            } catch (err: Throwable) {
                context.toast(err.errorMessage(if (isEditing) "保存失败，请稍后重试" else "发布失败，请稍后重试"))
            } finally {
                publishing = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(
            title = if (isEditing) "编辑帖子" else "发布动态",
            showBack = false,
            leftText = "取消",
            onLeft = { nav.pop() },
            rightText = if (publishing) "发布中…" else "发布",
            rightEnabled = canPublish(),
            onRight = { publish() },
        )

        if (loadingPost) {
            LoadingView()
            return@Column
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("分享校园里的新鲜事…", color = AppColors.TEXT_SECONDARY) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = AppColors.CARD_BG,
                    focusedContainerColor = AppColors.CARD_BG,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = AppColors.PRIMARY,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )

            // 图片九宫格
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                images.chunked(3).forEach { rowImages ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowImages.forEach { img ->
                            // 编辑模式下旧图只有远端 URL；本地图唯一键为 localPath，远端图为 remoteUrl
                            val imageKey = img.localPath.ifEmpty { img.remoteUrl }
                            Box {
                                AsyncImage(
                                    model = if (img.localPath.isNotEmpty()) {
                                        java.io.File(img.localPath)
                                    } else {
                                        Http.absoluteMediaUrl(img.remoteUrl)
                                    },
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AppColors.ICON_BG),
                                )
                                if (img.uploading) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0x66000000)),
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                                Text(
                                    "✕",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .background(Color(0x99000000), RoundedCornerShape(11.dp))
                                        .clickable {
                                            images = images.filter {
                                                (it.localPath.ifEmpty { it.remoteUrl }) != imageKey
                                            }
                                        },
                                )
                            }
                        }
                        repeat(3 - rowImages.size) { Spacer(Modifier.size(100.dp)) }
                    }
                }
                if (images.size < AppConfig.MAX_POST_IMAGES) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .background(AppColors.CARD_BG, RoundedCornerShape(10.dp))
                            .border(1.dp, AppColors.DIVIDER, RoundedCornerShape(10.dp))
                            .clickable { pickImagesClick() },
                    ) {
                        Text("＋", fontSize = 26.sp, color = AppColors.TEXT_SECONDARY)
                        Spacer(Modifier.height(4.dp))
                        Text("添加图片", fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
                    }
                }
            }

            // 标签
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        placeholder = {
                            Text(
                                "添加话题标签（最多 ${AppConfig.MAX_POST_TAGS} 个）",
                                fontSize = 13.sp,
                                color = AppColors.TEXT_SECONDARY,
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = AppColors.CARD_BG,
                            focusedContainerColor = AppColors.CARD_BG,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = AppColors.PRIMARY,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    )
                    Button(
                        onClick = { addTag() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PRIMARY),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("添加", fontSize = 13.sp)
                    }
                }
                if (tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { tag ->
                            Row(
                                modifier = Modifier
                                    .background(AppColors.TAG_BG, RoundedCornerShape(12.dp))
                                    .clickable { tags = tags.filter { it != tag } }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text("# $tag", fontSize = 12.sp, color = AppColors.PRIMARY)
                                Spacer(Modifier.width(4.dp))
                                Text("✕", fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
                            }
                        }
                    }
                }
            }

            Text(
                if (isEditing) {
                    "新版本审核通过前，旧版本继续公开。"
                } else {
                    "发布前请确认内容符合社区公约；演示环境内置内容审核，命中违禁词将被拦截。"
                },
                fontSize = 11.sp,
                color = AppColors.TEXT_SECONDARY,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 搜索页（对齐鸿蒙端 pages/Search.ets）。 */
@Composable
fun SearchScreen(nav: NavStack, initialKeyword: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val postRevision = AppState.postRevision
    var keyword by remember { mutableStateOf(initialKeyword) }
    var results by remember { mutableStateOf<List<Post>>(emptyList()) }
    var hotTopics by remember { mutableStateOf<List<String>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf("time") }

    fun doSearch() {
        val query = keyword.trim()
        if (query.isEmpty() || searching) return
        searching = true
        scope.launch {
            try {
                results = Api.listPosts(query, false)
                searched = true
            } catch (err: Throwable) {
                context.toast(err.errorMessage("搜索失败"))
            } finally {
                searching = false
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            hotTopics = Api.publicSettings().hot_topics
        } catch (_: Throwable) {
        }
        if (initialKeyword.isNotEmpty()) {
            doSearch()
        }
    }

    LaunchedEffect(postRevision) {
        if (searched && !searching && postRevision > 0) doSearch()
    }

    fun patchPost(updated: Post) {
        results = results.map { if (it.id == updated.id) updated else it }
    }

    fun sortedResults(): List<Post> {
        val list = when (sortMode) {
            "comments" -> results.sortedByDescending { it.comments }
            "likes" -> results.sortedByDescending { it.likes }
            "bookmarks" -> results.sortedByDescending { it.bookmarks }
            else -> results.sortedByDescending { it.created_at }
        }
        return list.sortedByDescending { it.pinned }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        ) {
            Text(
                "‹",
                fontSize = 26.sp,
                color = AppColors.TEXT_PRIMARY,
                modifier = Modifier.clickable { nav.pop() },
            )
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                placeholder = { Text("搜索帖子内容 / 标签", fontSize = 13.sp, color = AppColors.TEXT_SECONDARY) },
                singleLine = true,
                shape = RoundedCornerShape(19.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = AppColors.CARD_BG,
                    focusedContainerColor = AppColors.CARD_BG,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = AppColors.PRIMARY,
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { doSearch() },
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            )
            Text(
                "搜索",
                fontSize = 14.sp,
                color = AppColors.PRIMARY,
                modifier = Modifier.clickable { doSearch() },
            )
        }

        when {
            searching -> LoadingView(text = "搜索中…")
            !searched -> Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                if (hotTopics.isNotEmpty()) {
                    Text(
                        "热门话题",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TEXT_PRIMARY,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        hotTopics.take(6).forEach { topic ->
                            Text(
                                "# $topic",
                                fontSize = 13.sp,
                                color = AppColors.PRIMARY,
                                modifier = Modifier
                                    .background(AppColors.CARD_BG, RoundedCornerShape(14.dp))
                                    .clickable {
                                        keyword = topic
                                        doSearch()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            results.isEmpty() -> EmptyState(title = "没有找到相关帖子", desc = "换个关键词试试")
            else -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                ) {
                    searchSortOptions.forEach { (key, label) ->
                        val selected = sortMode == key
                        Text(
                            label,
                            fontSize = 13.sp,
                            color = if (selected) Color.White else AppColors.TEXT_SECONDARY,
                            modifier = Modifier
                                .background(
                                    if (selected) AppColors.PRIMARY else AppColors.CARD_BG,
                                    RoundedCornerShape(15.dp),
                                )
                                .clickable { sortMode = key }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    // 键值必须包含驱动卡片渲染的字段：否则点赞/收藏后 patchPost 拿到新数据，
                    // Compose 仍复用旧组件，操作栏数字与高亮不刷新
                    items(
                        sortedResults(),
                        key = { "${it.id}:${it.likes}:${it.comments}:${it.bookmarks}:${it.liked}:${it.bookmarked}:${it.pinned}" },
                    ) { post ->
                        PostCard(
                            post = post,
                            onOpen = { nav.push(Route.PostDetail(it.id)) },
                            onLike = { target ->
                                scope.launch {
                                    try {
                                        patchPost(Api.likePost(target.id).post)
                                    } catch (_: Throwable) {
                                        context.toast("操作失败")
                                    }
                                }
                            },
                            onBookmark = { target ->
                                scope.launch {
                                    try {
                                        patchPost(Api.bookmarkPost(target.id).post)
                                    } catch (_: Throwable) {
                                        context.toast("操作失败")
                                    }
                                }
                            },
                            onTag = { tag ->
                                keyword = tag
                                doSearch()
                            },
                            onAuthor = { nav.push(Route.UserProfile(it.author.id)) },
                        )
                    }
                }
            }
        }
    }
}

/** 通用内容列表：mode = mine（我的帖子）/ bookmarks（我的收藏）。 */
@Composable
fun ContentListScreen(nav: NavStack, mode: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val postRevision = AppState.postRevision
    var items by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf("") }

    val title = if (mode == "bookmarks") "我的收藏" else "我的帖子"

    suspend fun load() {
        loadError = ""
        try {
            items = if (mode == "bookmarks") Api.myBookmarks() else Api.listPosts("", true)
        } catch (err: Throwable) {
            loadError = err.errorMessage("加载失败")
        }
        loading = false
        refreshing = false
    }

    LaunchedEffect(Unit) { load() }
    LaunchedEffect(postRevision) {
        if (!loading && postRevision > 0) load()
    }

    fun patchPost(updated: Post) {
        items = items.map { if (it.id == updated.id) updated else it }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(title = title, onBack = { nav.pop() })

        when {
            loading -> LoadingView()
            loadError.isNotEmpty() && items.isEmpty() -> EmptyState(
                title = "加载失败",
                desc = loadError,
                actionText = "重试",
                onAction = {
                    scope.launch {
                        loading = true
                        load()
                    }
                },
            )

            else -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    refreshing = true
                    scope.launch { load() }
                },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                ) {
                    items(
                        items,
                        // 键值包含驱动卡片渲染的字段（与消息页未读冒泡同一类问题）
                        key = { "${it.id}:${it.likes}:${it.comments}:${it.bookmarks}:${it.liked}:${it.bookmarked}:${it.pinned}" },
                    ) { post ->
                        PostCard(
                            post = post,
                            onOpen = { nav.push(Route.PostDetail(it.id)) },
                            onLike = { target ->
                                scope.launch {
                                    try {
                                        patchPost(Api.likePost(target.id).post)
                                    } catch (_: Throwable) {
                                        context.toast("操作失败")
                                    }
                                }
                            },
                            onBookmark = { target ->
                                scope.launch {
                                    try {
                                        patchPost(Api.bookmarkPost(target.id).post)
                                    } catch (_: Throwable) {
                                        context.toast("操作失败")
                                    }
                                }
                            },
                            onTag = { nav.push(Route.Search(it)) },
                            onAuthor = { nav.push(Route.UserProfile(it.author.id)) },
                        )
                    }
                    if (items.isEmpty()) {
                        item {
                            EmptyState(title = if (mode == "bookmarks") "还没有收藏" else "还没有发布过帖子")
                        }
                    }
                }
            }
        }
    }
}

/** 校园公告（对齐鸿蒙端 pages/Announcements.ets）。 */
@Composable
fun AnnouncementsScreen(nav: NavStack) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }

    suspend fun load() {
        try {
            items = Api.listAnnouncements().filter { it.published }
        } catch (_: Throwable) {
        }
        loading = false
        refreshing = false
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(title = "校园公告", onBack = { nav.pop() })

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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        AnnouncementCard(
                            announcement = item,
                            onOpen = { nav.push(Route.AnnouncementDetail(it.id)) },
                        )
                    }
                    if (items.isEmpty()) {
                        item { EmptyState(title = "暂无公告") }
                    }
                }
            }
        }
    }
}
