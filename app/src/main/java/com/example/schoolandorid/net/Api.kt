package com.example.schoolandorid.net

import com.example.schoolandorid.model.*
import com.example.schoolandorid.state.AppState

/** 与鸿蒙端 Api.ets 相同的 /api/v1 端点封装。 */
object Api {

    // ---- 认证 ----
    suspend fun login(phone: String, password: String): AccountResp =
        Http.parse(Http.request("POST", "/api/v1/auth/login", mapOf("phone" to phone, "password" to password)))

    suspend fun smsCode(phone: String): SmsCodeResp =
        Http.parse(Http.request("POST", "/api/v1/auth/sms-code", mapOf("phone" to phone, "purpose" to "register")))

    suspend fun register(phone: String, code: String, password: String, nickname: String, inviteCode: String): AccountResp =
        Http.parse(
            Http.request(
                "POST", "/api/v1/auth/register",
                mapOf(
                    "phone" to phone, "code" to code, "password" to password,
                    "nickname" to nickname, "invite_code" to inviteCode,
                ),
            ),
        )

    suspend fun logout() {
        Http.request("POST", "/api/v1/auth/logout")
    }

    suspend fun me(): AccountResp = Http.parse(Http.request("GET", "/api/v1/me"))

    // ---- 帖子 ----
    suspend fun listPosts(query: String, mineOnly: Boolean): List<Post> {
        val params = mutableListOf<String>()
        if (query.isNotEmpty()) params.add("q=${java.net.URLEncoder.encode(query, "UTF-8")}")
        if (mineOnly) params.add("mine=1")
        val suffix = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        val resp: ItemsResp<Post> = Http.parse(Http.request("GET", "/api/v1/posts$suffix"))
        return resp.items
    }

    suspend fun getPost(id: Long): PostResp = Http.parse(Http.request("GET", "/api/v1/posts/$id"))

    suspend fun createPost(text: String, images: List<String>, tags: List<String>): CreatePostResp {
        val resp: CreatePostResp = Http.parse(
            Http.request("POST", "/api/v1/posts", mapOf("text" to text, "images" to images, "tags" to tags)),
        )
        AppState.publishPostChange()
        return resp
    }

    suspend fun likePost(id: Long): PostResp {
        val resp: PostResp = Http.parse(Http.request("POST", "/api/v1/posts/$id/like"))
        AppState.publishPostChange()
        return resp
    }

    suspend fun bookmarkPost(id: Long): PostResp {
        val resp: PostResp = Http.parse(Http.request("POST", "/api/v1/posts/$id/bookmark"))
        AppState.publishPostChange()
        return resp
    }

    suspend fun listComments(postId: Long): CommentsResp =
        Http.parse(Http.request("GET", "/api/v1/posts/$postId/comments"))

    suspend fun createComment(postId: Long, text: String, parentId: Long? = null): CreateCommentResp {
        val body = mutableMapOf<String, Any>("text" to text)
        if (parentId != null && parentId > 0) body["parent_id"] = parentId
        val resp: CreateCommentResp = Http.parse(Http.request("POST", "/api/v1/posts/$postId/comments", body))
        AppState.publishPostChange()
        return resp
    }

    suspend fun myBookmarks(): List<Post> {
        val resp: ItemsResp<Post> = Http.parse(Http.request("GET", "/api/v1/me/bookmarks"))
        return resp.items
    }

    // ---- 公告 / 设置 / 百宝箱 ----
    suspend fun listAnnouncements(): List<Announcement> {
        val resp: ItemsResp<Announcement> = Http.parse(Http.request("GET", "/api/v1/announcements"))
        return resp.items
    }

    suspend fun publicSettings(): PublicSettings = Http.parse(Http.request("GET", "/api/v1/settings/public"))

    suspend fun listTools(): List<CampusTool> {
        val resp: ItemsResp<CampusTool> = Http.parse(Http.request("GET", "/api/v1/tools"))
        return resp.items
    }

    // ---- 我的 ----
    suspend fun updateProfile(
        nickname: String, avatar: String, gender: String,
        realName: String, studentNo: String, className: String,
    ): AccountResp = Http.parse(
        Http.request(
            "PUT", "/api/v1/me/profile",
            mapOf(
                "nickname" to nickname, "avatar" to avatar, "gender" to gender,
                "real_name" to realName, "student_no" to studentNo, "class_name" to className,
            ),
        ),
    )

    suspend fun myVerification(): VerificationResp = Http.parse(Http.request("GET", "/api/v1/me/verification"))

    suspend fun submitVerification(materialUrl: String, realName: String, studentNo: String): VerificationResp =
        Http.parse(
            Http.request(
                "POST", "/api/v1/me/verification",
                mapOf("material_url" to materialUrl, "real_name" to realName, "student_no" to studentNo),
            ),
        )

    suspend fun changePassword(currentPassword: String, newPassword: String): ChangePasswordResp =
        Http.parse(
            Http.request(
                "POST", "/api/v1/me/password",
                mapOf("current_password" to currentPassword, "new_password" to newPassword),
            ),
        )

    suspend fun deleteAccount() {
        Http.request("DELETE", "/api/v1/me")
    }

    // ---- 通知 ----
    suspend fun notifications(): NotificationsResp = Http.parse(Http.request("GET", "/api/v1/me/notifications"))

    suspend fun markNotificationsRead(ids: List<Long>): UnreadResp {
        val resp: UnreadResp = Http.parse(Http.request("POST", "/api/v1/me/notifications/read", mapOf("ids" to ids)))
        AppState.setUnreadCount(resp.unread)
        AppState.publishMessageChange()
        return resp
    }

    // ---- 私信 ----
    suspend fun listDirectConversations(): DirectConversationsResp =
        Http.parse(Http.request("GET", "/api/v1/direct-conversations"))

    suspend fun getDirectConversation(id: Long): DirectConversationResp =
        Http.parse(Http.request("GET", "/api/v1/direct-conversations/$id"))

    suspend fun sendDirectMessage(id: Long, text: String, system: Boolean = false): SendDirectMessageResp {
        val resp: SendDirectMessageResp = Http.parse(
            Http.request("POST", "/api/v1/direct-conversations/$id/messages", mapOf("text" to text, "system" to system)),
        )
        // 发送响应即写入会话快照，消息卡片不等待下一次网络刷新
        AppState.appendDirectMessage(id, resp.message, resp.unlocked)
        AppState.publishMessageChange()
        return resp
    }

    suspend fun startDirectConversation(userId: Long): StartDirectConversationResp {
        val resp: StartDirectConversationResp = Http.parse(
            Http.request("POST", "/api/v1/direct-conversations", mapOf("user_id" to userId)),
        )
        AppState.publishMessageChange()
        return resp
    }

    /** 进入会话后持久化已读：服务端把对方消息置为 read，返回全量私信未读数直接刷新底栏。 */
    suspend fun markDirectConversationRead(id: Long): UnreadResp {
        val resp: UnreadResp = Http.parse(Http.request("POST", "/api/v1/direct-conversations/$id/read"))
        AppState.markDirectConversationRead(id, resp.unread)
        AppState.publishMessageChange()
        return resp
    }

    // ---- AI 问答 ----
    suspend fun aiModels(): List<AIModel> {
        val resp: ItemsResp<AIModel> = Http.parse(Http.request("GET", "/api/v1/ai/models"))
        return resp.items
    }

    suspend fun aiConversations(): AIConversationsResp = Http.parse(Http.request("GET", "/api/v1/ai/conversations"))

    suspend fun createAIConversation(title: String, model: String): AIConversationResp {
        val body = mutableMapOf<String, String>()
        if (title.isNotEmpty()) body["title"] = title
        if (model.isNotEmpty()) body["model"] = model
        return Http.parse(Http.request("POST", "/api/v1/ai/conversations", body))
    }

    suspend fun askAI(id: Long, text: String, model: String): AskAIResp {
        val body = mutableMapOf<String, String>("text" to text)
        if (model.isNotEmpty()) body["model"] = model
        return Http.parse(Http.request("POST", "/api/v1/ai/conversations/$id/messages", body))
    }
}

/** 通用 items 包装。 */
data class ItemsResp<T>(val items: List<T> = emptyList())
