package com.example.schoolandorid

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.schoolandorid.config.AppColors
import com.example.schoolandorid.nav.NavStack
import com.example.schoolandorid.nav.Route
import com.example.schoolandorid.state.AppState
import com.example.schoolandorid.state.NotificationSync
import com.example.schoolandorid.ui.screens.AIAssistantScreen
import com.example.schoolandorid.ui.screens.AIChatScreen
import com.example.schoolandorid.ui.screens.AboutScreen
import com.example.schoolandorid.ui.screens.AccountSettingsScreen
import com.example.schoolandorid.ui.screens.AnnouncementsScreen
import com.example.schoolandorid.ui.screens.ChatScreen
import com.example.schoolandorid.ui.screens.ComposeScreen
import com.example.schoolandorid.ui.screens.ContentListScreen
import com.example.schoolandorid.ui.screens.EditProfileScreen
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
import com.example.schoolandorid.ui.screens.VerificationScreen
import com.example.schoolandorid.ui.theme.SchoolAndoridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            SchoolAndoridTheme {
                AppRoot()
            }
        }
    }
}

/**
 * 应用唯一入口（对齐鸿蒙端 pages/Main.ets + pages/PageMap.ets）：
 * 冷启动恢复会话后决定首帧路由，所有页面由 NavStack 统一调度。
 */
@Composable
fun AppRoot() {
    val nav = remember { NavStack() }
    var ready by remember { mutableStateOf(false) }

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
                is Route.Tabs -> TabsScreen(nav)
                is Route.PostDetail -> PostDetailScreen(nav, route.id)
                is Route.NotificationDetail -> NotificationDetailScreen(nav, route.notification)
                is Route.Notifications -> NotificationsScreen(nav)
                is Route.LegalDocument -> LegalDocumentScreen(nav, route.type)
                is Route.NotificationSettings -> NotificationSettingsScreen(nav)
                is Route.About -> AboutScreen(nav)
                is Route.Compose -> ComposeScreen(nav)
                is Route.Search -> SearchScreen(nav, route.keyword)
                is Route.Announcements -> AnnouncementsScreen(nav)
                is Route.Tools -> ToolsTab(nav)
                is Route.AI -> AIAssistantScreen(nav)
                is Route.Chat -> ChatScreen(nav, route.id, route.name)
                is Route.AIChat -> AIChatScreen(nav, route.id, route.title)
                is Route.ContentList -> ContentListScreen(nav, route.mode)
                is Route.EditProfile -> EditProfileScreen(nav)
                is Route.Verification -> VerificationScreen(nav)
                is Route.AccountSettings -> AccountSettingsScreen(nav)
                null -> Unit
            }
        }
    }
}
