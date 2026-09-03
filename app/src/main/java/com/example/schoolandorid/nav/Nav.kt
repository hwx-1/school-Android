package com.example.schoolandorid.nav

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.schoolandorid.model.AppNotification

/** 路由（对齐鸿蒙端 PageMap.ets 的 NavDestination 路由表）。 */
sealed interface Route {
    data object Login : Route
    data object Register : Route
    data object ForgotPassword : Route
    data object Tabs : Route
    data class PostDetail(val id: Long) : Route
    data class NotificationDetail(val notification: AppNotification) : Route
    data object Notifications : Route
    data class LegalDocument(val type: String) : Route
    data object NotificationSettings : Route
    data object About : Route
    /** 发布 / 编辑帖子：postId > 0 时为编辑模式（对齐 web 端 /compose/:postId）。 */
    data class Compose(val postId: Long = 0) : Route
    data class Search(val keyword: String) : Route
    data object Announcements : Route
    data class AnnouncementDetail(val id: Long) : Route
    /** 公开主页（对齐 web 端 /users/:id）。 */
    data class UserProfile(val id: Long) : Route
    data object Tools : Route
    data object AI : Route
    data class Chat(val id: Long, val name: String) : Route
    data class AIChat(val id: Long, val title: String) : Route
    data class ContentList(val mode: String) : Route
    data object EditProfile : Route
    data object Verification : Route
    data object AccountSettings : Route
    /** 我的申诉（对齐鸿蒙端 pages/Appeals.ets）：punishmentId > 0 时可直接提交该处罚的申诉。 */
    data class Appeals(val punishmentId: Long = 0) : Route
}

/** 页面栈（对齐鸿蒙端 NavPathStack 的 push / pop / replace 语义）。 */
class NavStack {
    val stack: SnapshotStateList<Route> = mutableStateListOf()

    val current: Route? get() = stack.lastOrNull()

    fun push(route: Route) {
        stack.add(route)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.size - 1)
    }

    fun replace(route: Route) {
        if (stack.isEmpty()) {
            stack.add(route)
        } else {
            stack[stack.size - 1] = route
        }
    }

    fun resetTo(route: Route) {
        stack.clear()
        stack.add(route)
    }
}
