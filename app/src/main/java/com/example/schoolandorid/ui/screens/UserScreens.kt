package com.example.schoolandorid.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.schoolandorid.config.AppColors
import com.example.schoolandorid.model.Announcement
import com.example.schoolandorid.model.Post
import com.example.schoolandorid.model.PublicAccount
import com.example.schoolandorid.nav.NavStack
import com.example.schoolandorid.nav.Route
import com.example.schoolandorid.net.Api
import com.example.schoolandorid.net.Http
import com.example.schoolandorid.state.AppState
import com.example.schoolandorid.ui.components.AvatarView
import com.example.schoolandorid.ui.components.BadgeView
import com.example.schoolandorid.ui.components.EmptyState
import com.example.schoolandorid.ui.components.LoadingView
import com.example.schoolandorid.ui.components.NavBar
import com.example.schoolandorid.ui.components.PostCard
import com.example.schoolandorid.ui.components.ReportDialog
import com.example.schoolandorid.ui.components.ReportTarget
import com.example.schoolandorid.ui.errorMessage
import com.example.schoolandorid.ui.toast
import com.example.schoolandorid.util.TimeUtil
import kotlinx.coroutines.launch

/** 公开主页（对齐 web 端 PublicProfilePage）：公开资料 + TA 的公开帖子 + 私信 / 举报。 */
@Composable
fun UserProfileScreen(nav: NavStack, userId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val postRevision = AppState.postRevision
    var user by remember { mutableStateOf<PublicAccount?>(null) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }

    val isSelf = user?.id == (AppState.account?.id ?: -1L)

    suspend fun load() {
        loadError = ""
        try {
            val resp = Api.getUser(userId)
            user = resp.user
            posts = resp.posts.filter { it.status == "public" }
        } catch (err: Throwable) {
            loadError = err.errorMessage("加载失败")
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }
    LaunchedEffect(postRevision) {
        if (!loading && postRevision > 0) load()
    }

    fun patchPost(updated: Post) {
        posts = posts.map { if (it.id == updated.id) updated else it }
    }

    fun startChat() {
        val u = user ?: return
        if (busy) return
        busy = true
        scope.launch {
            try {
                val resp = Api.startDirectConversation(u.id)
                nav.push(Route.Chat(resp.item.id, u.nickname))
            } catch (err: Throwable) {
                context.toast(err.errorMessage("发起私信失败，请稍后重试"))
            } finally {
                busy = false
            }
        }
    }

    if (reportOpen) {
        ReportDialog(target = ReportTarget("user", userId), onClose = { reportOpen = false })
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(title = "公开主页", onBack = { nav.pop() })

        when {
            loading -> LoadingView()
            loadError.isNotEmpty() || user == null -> EmptyState(
                title = "没有找到这个用户",
                desc = loadError,
                actionText = "重试",
                onAction = {
                    scope.launch {
                        loading = true
                        load()
                    }
                },
            )

            else -> {
                val u = user!!
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                ) {
                    item(key = "profile") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.CARD_BG, RoundedCornerShape(14.dp))
                                .padding(16.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AvatarView(avatar = u.avatar, nickname = u.nickname, diameter = 64)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            u.nickname,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.TEXT_PRIMARY,
                                        )
                                        if (u.verified) {
                                            BadgeView(badge = u.badge ?: "org")
                                        }
                                    }
                                    Text(
                                        u.gender.ifEmpty { "未设置性别" },
                                        fontSize = 13.sp,
                                        color = AppColors.TEXT_SECONDARY,
                                    )
                                    Text("只展示公开资料与已公开帖子", fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
                                }
                            }
                            if (!isSelf) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { startChat() },
                                        enabled = !busy,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PRIMARY),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp),
                                    ) {
                                        Text("发私信", fontSize = 14.sp)
                                    }
                                    Button(
                                        onClick = { reportOpen = true },
                                        enabled = !busy,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.ACTION_BG),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp),
                                    ) {
                                        Text("举报", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY)
                                    }
                                }
                            }
                        }
                    }

                    item(key = "posts_title") {
                        Text(
                            "TA 的帖子（${posts.size}）",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TEXT_PRIMARY,
                        )
                    }

                    items(
                        posts,
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
                        )
                    }

                    if (posts.isEmpty()) {
                        item { EmptyState(title = "暂无公开帖子") }
                    }
                }
            }
        }
    }
}

/** 公告详情（对齐 web 端 /announcements/:id）：全文 + 配图 + 外链。 */
@Composable
fun AnnouncementDetailScreen(nav: NavStack, announcementId: Long) {
    val context = LocalContext.current
    var announcement by remember { mutableStateOf<Announcement?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            announcement = Api.getAnnouncement(announcementId).announcement
        } catch (err: Throwable) {
            loadError = err.errorMessage("公告加载失败")
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(title = "公告详情", onBack = { nav.pop() })

        when {
            loading -> LoadingView()
            loadError.isNotEmpty() || announcement == null -> EmptyState(title = "公告不存在或已下线", desc = loadError)
            else -> {
                val item = announcement!!
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.CARD_BG, RoundedCornerShape(14.dp))
                            .padding(18.dp),
                    ) {
                        Text(
                            item.title,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TEXT_PRIMARY,
                        )
                        Text(
                            TimeUtil.format(item.published_at ?: item.created_at),
                            fontSize = 12.sp,
                            color = AppColors.TEXT_SECONDARY,
                        )
                        if (!item.image_url.isNullOrEmpty()) {
                            AsyncImage(
                                model = Http.absoluteMediaUrl(item.image_url),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppColors.ICON_BG),
                            )
                        }
                        Text(
                            item.body,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = AppColors.TEXT_PRIMARY,
                        )
                        if (!item.link_url.isNullOrEmpty()) {
                            Text(
                                item.link_text?.ifEmpty { "查看相关链接" } ?: "查看相关链接",
                                fontSize = 14.sp,
                                color = AppColors.PRIMARY,
                                modifier = Modifier.clickable {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link_url)))
                                    } catch (_: Exception) {
                                        context.toast("无法打开该链接")
                                    }
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
