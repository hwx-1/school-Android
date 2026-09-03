package com.example.schoolandorid

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.schoolandorid.config.AppColors
import com.example.schoolandorid.nav.NavStack
import com.example.schoolandorid.nav.Route
import com.example.schoolandorid.state.AppState
import com.example.schoolandorid.state.NotificationSync
import com.example.schoolandorid.ui.screens.AIChatScreen
import com.example.schoolandorid.ui.screens.AboutScreen
import com.example.schoolandorid.ui.screens.AccountSettingsScreen
import com.example.schoolandorid.ui.screens.AnnouncementDetailScreen
import com.example.schoolandorid.ui.screens.AnnouncementsScreen
import com.example.schoolandorid.ui.screens.AppealsScreen
import com.example.schoolandorid.ui.screens.ChatScreen
import com.example.schoolandorid.ui.screens.ComposeScreen
import com.example.schoolandorid.ui.screens.ContentListScreen
import com.example.schoolandorid.ui.screens.EditProfileScreen
import com.example.schoolandorid.ui.screens.ForgotPasswordScreen
import com.example.schoolandorid.ui.screens.LegalDocumentScreen
import com.example.schoolandorid.ui.screens.LoginScreen
import com.example.schoolandorid.ui.screens.MessagesTab
import com.example.schoolandorid.ui.screens.NotificationDetailScreen
import com.example.schoolandorid.ui.screens.NotificationSettingsScreen
import com.example.schoolandorid.ui.screens.NotificationsScreen
import com.example.schoolandorid.ui.screens.PostDetailScreen
import com.example.schoolandorid.ui.screens.RegisterScreen
import com.example.schoolandorid.ui.screens.SearchScreen
import com.example.schoolandorid.ui.screens.TabsScreen
import com.example.schoolandorid.ui.screens.ToolsTab
import com.example.schoolandorid.ui.screens.UserProfileScreen
import com.example.schoolandorid.ui.screens.VerificationScreen
import com.example.schoolandorid.ui.theme.SchoolAndoridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.initialize(applicationContext)
        Coil.setImageLoader(buildImageLoader(applicationContext))
        enableEdgeToEdge()
        setContent {
            SchoolAndoridTheme {
                AppRoot()
            }
        }
    }
}

/**
 * 全局图片加载器：磁盘缓存避免重复下载，crossfade 让图片加载完成后淡入，
 * 缓解首屏与分页图片“等加载 / 一片灰”的观感。
 */
private fun buildImageLoader(context: Context): ImageLoader =
    ImageLoader.Builder(context)
        .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.25).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(256L * 1024 * 1024)
                .build()
        }
        .crossfade(true)
        .build()

/**
 * 应用唯一入口（对齐鸿蒙端 pages/Main.ets + pages/PageMap.ets）：
 * 冷启动恢复会话后决定首帧路由，所有页面由 NavStack 统一调度。
 */
@Composable
fun AppRoot() {
    val nav = remember { NavStack() }
    var ready by remember { mutableStateOf(false) }

    // 系统返回手势 / 返回键：有上一页时弹栈，否则交给系统默认（退出）
    BackHandler(enabled = nav.stack.size > 1) { nav.pop() }

    // 前台轮询通知：跟随生命周期启停
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> NotificationSync.start()
                Lifecycle.Event.ON_STOP -> NotificationSync.stop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            NotificationSync.stop()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val loggedIn = AppState.restore()
        nav.resetTo(if (loggedIn) Route.Tabs else Route.Login)
        ready = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.PAGE_BG)
            .safeDrawingPadding(),
    ) {
        if (!ready) {
            CircularProgressIndicator(
                color = AppColors.PRIMARY,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
            )
        } else {
            when (val route = nav.current) {
                is Route.Login -> LoginScreen(nav)
                is Route.Register -> RegisterScreen(nav)
                is Route.ForgotPassword -> ForgotPasswordScreen(nav)
                is Route.Tabs -> TabsScreen(nav)
                is Route.PostDetail -> PostDetailScreen(nav, route.id)
                is Route.NotificationDetail -> NotificationDetailScreen(nav, route.notification)
                is Route.Notifications -> NotificationsScreen(nav)
                is Route.LegalDocument -> LegalDocumentScreen(nav, route.type)
                is Route.NotificationSettings -> NotificationSettingsScreen(nav)
                is Route.About -> AboutScreen(nav)
                is Route.Compose -> ComposeScreen(nav, route.postId)
                is Route.Search -> SearchScreen(nav, route.keyword)
                is Route.Announcements -> AnnouncementsScreen(nav)
                is Route.AnnouncementDetail -> AnnouncementDetailScreen(nav, route.id)
                is Route.UserProfile -> UserProfileScreen(nav, route.id)
                is Route.Tools -> ToolsTab(nav)
                is Route.AI -> AIChatScreen(nav, null, "")
                is Route.Chat -> ChatScreen(nav, route.id, route.name)
                is Route.AIChat -> key(route.id) { AIChatScreen(nav, route.id, route.title) }
                is Route.ContentList -> ContentListScreen(nav, route.mode)
                is Route.EditProfile -> EditProfileScreen(nav)
                is Route.Verification -> VerificationScreen(nav)
                is Route.AccountSettings -> AccountSettingsScreen(nav)
                is Route.Appeals -> AppealsScreen(nav, route.punishmentId)
                null -> Unit
            }
        }
    }
}
