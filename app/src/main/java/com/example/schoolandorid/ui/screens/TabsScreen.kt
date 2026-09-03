package com.example.schoolandorid.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.schoolandorid.model.Announcement
import com.example.schoolandorid.model.CampusTool
import com.example.schoolandorid.model.Post
import com.example.schoolandorid.model.PublicSettings
import com.example.schoolandorid.nav.NavStack
import com.example.schoolandorid.nav.Route
import com.example.schoolandorid.net.Api
import com.example.schoolandorid.net.ApiError
import com.example.schoolandorid.state.AppState
import com.example.schoolandorid.ui.components.EmptyState
import com.example.schoolandorid.ui.components.LoadingView
import com.example.schoolandorid.ui.components.NavBar
import com.example.schoolandorid.ui.components.PostCard
import com.example.schoolandorid.ui.components.UnreadBadge
import com.example.schoolandorid.ui.errorMessage
import com.example.schoolandorid.ui.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "TabsScreen"

private data class TabSpec(val label: String, val icon: ImageVector)

private val tabSpecs = listOf(
    TabSpec("首页", Icons.Outlined.Home),
    TabSpec("百宝箱", Icons.Outlined.Widgets),
    TabSpec("消息", Icons.Outlined.Notifications),
    TabSpec("我的", Icons.Outlined.Person),
)

/** 主界面：四个 Tab + 发布入口（对齐鸿蒙端 pages/Index.ets TabsPage）。 */
@Composable
fun TabsScreen(nav: NavStack) {
    val context = LocalContext.current
    var currentTab by remember { mutableIntStateOf(0) }
    val unreadCount = AppState.unreadCount
    val directUnreadCount = AppState.directUnreadCount
    val badgeEnabled = AppState.notificationBadgeEnabled
    var observedUnread by remember { mutableIntStateOf(-1) }
    var hasBaseline by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // 未读数变化：首次同步只建立基线，之后前台收到新消息时轻提示
    LaunchedEffect(unreadCount) {
        val previous = observedUnread
        observedUnread = unreadCount
        if (!hasBaseline) {
            hasBaseline = true
            return@LaunchedEffect
        }
        if (AppState.inAppNotificationEnabled && previous >= 0 && unreadCount > previous) {
            context.toast("收到 ${unreadCount - previous} 条新的互动消息")
        }
    }

    suspend fun refreshUnread() {
        try {
            val response = Api.notifications()
            AppState.setUnreadCount(response.unread)
        } catch (e: Exception) {
            Log.e(TAG, "refreshUnread failed: ${e.message}")
        }
        try {
            val response = Api.listDirectConversations()
            AppState.setDirectUnreadCount(response.unread)
        } catch (e: Exception) {
            Log.e(TAG, "refresh direct unread failed: ${e.message}")
        }
    }

    LaunchedEffect(Unit) {
        AppState.requestPostRefresh()
        refreshUnread()
    }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    0 -> HomeTab(nav)
                    1 -> ToolsTab(nav)
                    2 -> MessagesTab(nav)
                    else -> MineTab(nav)
                }
            }
            NavigationBar(containerColor = AppColors.CARD_BG) {
                tabSpecs.forEachIndexed { index, spec ->
                    val badge = if (index == 2 && badgeEnabled) unreadCount + directUnreadCount else 0
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = {
                            currentTab = index
                            when (index) {
                                0 -> AppState.requestPostRefresh()
                                2 -> {
                                    AppState.requestMessageRefresh()
                                    scope.launch { refreshUnread() }
                                }
                            }
                        },
                        icon = {
                            Box {
                                Icon(spec.icon, contentDescription = spec.label, modifier = Modifier.size(22.dp))
                                if (badge > 0) {
                                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(start = 14.dp, bottom = 8.dp)) {
                                        UnreadBadge(count = badge)
                                    }
                                }
                            }
                        },
                        label = { Text(spec.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppColors.PRIMARY,
                            selectedTextColor = AppColors.PRIMARY,
                            unselectedIconColor = AppColors.TEXT_SECONDARY,
                            unselectedTextColor = AppColors.TEXT_SECONDARY,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        }

        // 发布按钮
        FloatingActionButton(
            onClick = { nav.push(Route.Compose()) },
            containerColor = AppColors.PRIMARY,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 84.dp)
                .size(52.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "发布", modifier = Modifier.size(24.dp))
        }
    }
}

/** 排序选项 */
private val sortOptions = listOf(
    "time" to "最新",
    "comments" to "评论",
    "likes" to "点赞",
    "bookmarks" to "收藏",
)

private const val HOME_PAGE_SIZE = 15

/** 首页信息流（对齐鸿蒙端 components/tabs/HomeTab.ets）。 */
@Composable
fun HomeTab(nav: NavStack) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val postRevision = AppState.postRevision
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf("") }
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var settings by remember { mutableStateOf(PublicSettings()) }
    var tools by remember { mutableStateOf<List<CampusTool>>(emptyList()) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var hasMore by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf("time") }
    val listState = rememberLazyListState()
    val likingIds = remember { mutableSetOf<Long>() }
    val bookmarkingIds = remember { mutableSetOf<Long>() }

    suspend fun loadPosts() {
        loadError = ""
        try {
            val page = Api.listPostsPage("", false, limit = HOME_PAGE_SIZE, offset = 0)
            posts = page.items
            hasMore = page.has_more
        } catch (err: Throwable) {
            loadError = err.errorMessage("加载失败")
        }
    }

    suspend fun loadMore() {
        if (!hasMore || loadingMore) return
        loadingMore = true
        try {
            val page = Api.listPostsPage("", false, limit = HOME_PAGE_SIZE, offset = posts.size)
            posts = posts + page.items
            hasMore = page.has_more
        } catch (err: Throwable) {
            context.toast(err.errorMessage("加载失败"))
        } finally {
            loadingMore = false
        }
    }

    suspend fun loadAll() {
        loadPosts()
        // 轮播图 / 话题 / 工具失败不阻塞信息流
        try {
            announcements = Api.listAnnouncements().filter { it.published }
        } catch (_: Throwable) {
        }
        try {
            settings = Api.publicSettings()
        } catch (_: Throwable) {
        }
        try {
            tools = Api.listTools().filter { it.enabled }.sortedByDescending { it.weight }.take(7)
        } catch (_: Throwable) {
        }
        loading = false
        refreshing = false
    }

    LaunchedEffect(Unit) { loadAll() }

    LaunchedEffect(postRevision) {
        if (!loading && postRevision > 0) loadPosts()
    }

    // 接近底部时自动加载下一页（提前 4 个 item 触发，避免到底才加载）
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            hasMore && !loadingMore && total > 0 && last >= total - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) loadMore()
    }

    fun patchPost(updated: Post) {
        posts = posts.map { if (it.id == updated.id) updated else it }
    }

    fun handleLike(post: Post) {
        if (!likingIds.add(post.id)) return
        patchPost(
            post.copy(
                likes = maxOf(0, post.likes + if (post.liked) -1 else 1),
                liked = !post.liked,
            ),
        )
        scope.launch {
            try {
                patchPost(Api.likePost(post.id).post)
            } catch (err: Throwable) {
                patchPost(post)
                context.toast(err.errorMessage("操作失败"))
            } finally {
                likingIds.remove(post.id)
            }
        }
    }

    fun handleBookmark(post: Post) {
        if (!bookmarkingIds.add(post.id)) return
        patchPost(
            post.copy(
                bookmarks = maxOf(0, post.bookmarks + if (post.bookmarked) -1 else 1),
                bookmarked = !post.bookmarked,
            ),
        )
        scope.launch {
            try {
                val resp = Api.bookmarkPost(post.id)
                patchPost(resp.post)
                context.toast(if (resp.post.bookmarked) "已收藏" else "已取消收藏")
            } catch (err: Throwable) {
                patchPost(post)
                context.toast(err.errorMessage("操作失败"))
            } finally {
                bookmarkingIds.remove(post.id)
            }
        }
    }

    fun sortedPosts(): List<Post> {
        val list = when (sortMode) {
            "comments" -> posts.sortedByDescending { it.comments }
            "likes" -> posts.sortedByDescending { it.likes }
            "bookmarks" -> posts.sortedByDescending { it.bookmarks }
            else -> posts.sortedByDescending { it.created_at }
        }
        return list.sortedByDescending { it.pinned }
    }

    fun openSearch(tag: String) = nav.push(Route.Search(tag))

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

    fun toolIcon(tool: CampusTool): String =
        tool.icon.ifEmpty { if (tool.type == "link") "🔗" else "🧰" }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(title = "沈大社区", showBack = false)

        // 搜索入口
        Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(38.dp)
                    .weight(1f)
                    .background(AppColors.CARD_BG, RoundedCornerShape(19.dp))
                    .clickable { nav.push(Route.Search("")) }
                    .padding(start = 14.dp),
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = AppColors.TEXT_SECONDARY,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("搜索帖子 / 话题", fontSize = 13.sp, color = AppColors.TEXT_SECONDARY)
            }
        }

        // 排序筛选栏
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        ) {
            sortOptions.forEach { (key, label) ->
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

        when {
            loading -> LoadingView()
            loadError.isNotEmpty() && posts.isEmpty() -> EmptyState(
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

            else -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    refreshing = true
                    scope.launch { loadAll() }
                },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                ) {
                    // 顶部轮播图：最新公告
                    if (announcements.isNotEmpty()) {
                        item(key = "announcement_swiper") {
                            AnnouncementSwiper(
                                announcements = announcements.take(3),
                                onClick = { nav.push(Route.Announcements) },
                            )
                        }
                    }

                    // 常用服务导航：AI 助手 + 常用工具，4 列最多 2 排
                    item(key = "service_grid") {
                        ServiceGrid(
                            tools = tools,
                            toolIcon = { toolIcon(it) },
                            onOpenAI = { nav.push(Route.AI) },
                            onOpenTool = { openTool(it) },
                        )
                    }

                    // 热门话题
                    if (settings.hot_topics.isNotEmpty()) {
                        item(key = "hot_topics") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                settings.hot_topics.forEach { topic ->
                                    Text(
                                        "# $topic",
                                        fontSize = 12.sp,
                                        color = AppColors.PRIMARY,
                                        modifier = Modifier
                                            .background(AppColors.CARD_BG, RoundedCornerShape(14.dp))
                                            .clickable { openSearch(topic) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }

                    items(
                        sortedPosts(),
                        // 键值包含驱动卡片渲染的字段：点赞/收藏/置顶变化时强制刷新卡片
                        key = { "${it.id}_${it.likes}_${it.comments}_${it.bookmarks}_${it.liked}_${it.bookmarked}_${it.pinned}" },
                    ) { post ->
                        PostCard(
                            post = post,
                            onOpen = { nav.push(Route.PostDetail(it.id)) },
                            onLike = { handleLike(it) },
                            onBookmark = { handleBookmark(it) },
                            onTag = { openSearch(it) },
                            onAuthor = { nav.push(Route.UserProfile(it.author.id)) },
                        )
                    }

                    if (posts.isEmpty()) {
                        item(key = "empty") {
                            EmptyState(title = "还没有帖子", desc = "来发第一条校园动态吧")
                        }
                    } else {
                        item(key = "footer") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            ) {
                                when {
                                    loadingMore -> {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            CircularProgressIndicator(
                                                color = AppColors.PRIMARY,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                            )
                                            Text("正在加载更多…", fontSize = 13.sp, color = AppColors.TEXT_SECONDARY)
                                        }
                                    }
                                    hasMore -> Text("继续下滑加载更多", fontSize = 12.sp, color = AppColors.TEXT_SECONDARY)
                                    else -> Text("已经看到这里了", fontSize = 12.sp, color = AppColors.TEXT_SECONDARY)
                                }
                            }
                        }
                    }

                    item(key = "bottom_spacer") { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

/** 顶部公告轮播（对齐鸿蒙端 Swiper：自动播放 + 循环 + 圆点指示器）。 */
@Composable
private fun AnnouncementSwiper(announcements: List<Announcement>, onClick: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { announcements.size })

    LaunchedEffect(announcements.size) {
        if (announcements.size <= 1) return@LaunchedEffect
        while (true) {
            delay(4000)
            val next = (pagerState.currentPage + 1) % announcements.size
            pagerState.animateScrollToPage(next)
        }
    }

    Box {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) { page ->
            val item = announcements[page]
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(AppColors.PRIMARY, AppColors.PRIMARY_LIGHT)))
                    .clickable { onClick() }
                    .padding(18.dp),
            ) {
                Text(
                    item.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    item.summary,
                    fontSize = 12.sp,
                    color = Color(0xE6FFFFFF),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // 圆点指示器
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
        ) {
            repeat(announcements.size) { index ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == index) Color.White else Color(0x80FFFFFF)),
                )
            }
        }
    }
}

/** 常用服务导航：AI 助手 + 常用工具，4 列最多 2 排。 */
@Composable
private fun ServiceGrid(
    tools: List<CampusTool>,
    toolIcon: (CampusTool) -> String,
    onOpenAI: () -> Unit,
    onOpenTool: (CampusTool) -> Unit,
) {
    val cells: List<@Composable () -> Unit> = buildList {
        add {
            ServiceCell(
                icon = {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = AppColors.PRIMARY,
                        modifier = Modifier.size(24.dp),
                    )
                },
                name = "AI 助手",
                onClick = onOpenAI,
            )
        }
        tools.forEach { tool ->
            add {
                ServiceCell(
                    icon = { Text(toolIcon(tool), fontSize = 24.sp) },
                    name = tool.name,
                    onClick = { onOpenTool(tool) },
                )
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cells.chunked(4).take(2).forEach { rowCells ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowCells.forEach { cell ->
                    Box(modifier = Modifier.weight(1f)) { cell() }
                }
                repeat(4 - rowCells.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ServiceCell(icon: @Composable () -> Unit, name: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.CARD_BG, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
    ) {
        icon()
        Text(
            name,
            fontSize = 12.sp,
            color = AppColors.TEXT_PRIMARY,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
