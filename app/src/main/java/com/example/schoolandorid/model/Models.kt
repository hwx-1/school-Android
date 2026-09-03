package com.example.schoolandorid.model

/** 与服务端 /api/v1 契约严格对应的数据模型（对齐鸿蒙端 model/Types.ets）。
 *  字段名保持 snake_case，Gson 直接映射。 */

data class PublicAccount(
    val id: Long = 0,
    val nickname: String = "",
    val avatar: String = "",
    val gender: String = "",
    val verified: Boolean = false,
    /** 徽标类型：official=官方(红) org=认证机构(蓝) admin=管理员发布(绿) */
    val badge: String? = null,
)

data class Account(
    val id: Long = 0,
    val nickname: String = "",
    val avatar: String = "",
    val gender: String = "",
    val verified: Boolean = false,
    val badge: String? = null,
    val phone: String? = null,
    val real_name: String? = null,
    val student_no: String? = null,
    val class_name: String? = null,
    val profile_done: Boolean = false,
    val status: String = "",
    val created_at: String = "",
)

data class Post(
    val id: Long = 0,
    val author: PublicAccount = PublicAccount(),
    val text: String = "",
    val images: List<String>? = null,
    val tags: List<String>? = null,
    val status: String = "",
    val pinned: Boolean = false,
    val likes: Int = 0,
    val comments: Int = 0,
    val bookmarks: Int = 0,
    val liked: Boolean = false,
    val bookmarked: Boolean = false,
    val created_at: String = "",
    val updated_at: String = "",
)

data class CommentItem(
    val id: Long = 0,
    val post_id: Long = 0,
    val parent_id: Long? = null,
    val author: PublicAccount = PublicAccount(),
    val text: String = "",
    val image: String? = null,
    val status: String = "",
    val deleted: Boolean = false,
    val created_at: String = "",
)

data class Announcement(
    val id: Long = 0,
    val title: String = "",
    val summary: String = "",
    val body: String = "",
    val image_url: String? = null,
    val link_url: String? = null,
    val link_text: String? = null,
    val published: Boolean = false,
    val created_at: String = "",
    val updated_at: String = "",
    val published_at: String? = null,
)

data class CampusTool(
    val id: Long = 0,
    val name: String = "",
    val type: String = "",
    val icon: String = "",
    val url: String? = null,
    val weight: Int = 0,
    val enabled: Boolean = false,
)

data class AppNotification(
    val id: Long = 0,
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val ref_type: String? = null,
    val ref_id: Long? = null,
    val read: Boolean = false,
    val created_at: String = "",
)

data class DirectMessage(
    val id: Long = 0,
    val sender_id: Long = 0,
    val text: String = "",
    val system: Boolean = false,
    val status: String = "",
    val created_at: String = "",
)

data class DirectConversationItem(
    val id: Long = 0,
    val other: PublicAccount = PublicAccount(),
    val unlocked: Boolean = false,
    val messages: List<DirectMessage> = emptyList(),
    val updated_at: String = "",
    val unread_count: Int = 0,
)

/** 私信会话列表响应：条目 + 全量私信未读数。 */
data class DirectConversationsResp(
    val items: List<DirectConversationItem> = emptyList(),
    val unread: Int = 0,
)

data class PublicSettings(
    val hot_topics: List<String> = emptyList(),
    val greeting: String = "",
)

data class ModerationInfo(
    val pass: Boolean = false,
    val category: String? = null,
    val reason: String? = null,
    val dev_mode: Boolean = false,
)

data class AIModel(
    val id: Long = 0,
    val name: String = "",
    val model: String = "",
    val enabled: Boolean = false,
    val public: Boolean = false,
)

data class AIMessage(
    val id: Long = 0,
    val role: String = "",
    val text: String = "",
    /** 模型给出最终答案前的思考过程（流式 reasoning_content）。 */
    val reasoning: String? = null,
    val model: String? = null,
    val source: String? = null,
    /** 知识库命中待确认：true 时需要用户选择「是 / 否，联网搜索」 */
    val needs_feedback: Boolean = false,
    /** 反馈结果：yes=知识库答案有帮助，no=已联网重答 */
    val feedback: String? = null,
    val created_at: String = "",
)

/** SSE 流式回答事件：type = thinking / text / done / error。 */
data class AIStreamEvent(
    val type: String = "",
    val delta: String? = null,
    val message: String? = null,
    val user_message: AIMessage? = null,
    val answer: AIMessage? = null,
    val remaining: Int = 0,
)

data class AIConversation(
    val id: Long = 0,
    val title: String = "",
    val model: String = "",
    val messages: List<AIMessage> = emptyList(),
    val created_at: String = "",
)

/** 申诉记录（对齐鸿蒙端 Appeal）。 */
data class Appeal(
    val id: Long = 0,
    val punishment_id: Long = 0,
    val account_id: Long = 0,
    /** ban=账号封禁申诉，其余为禁言申诉 */
    val kind: String = "",
    val reason: String = "",
    /** pending=待处理 lifted=处罚已解除 upheld=维持处罚 */
    val status: String = "",
    val result: String? = null,
    val created_at: String = "",
    val resolved_at: String? = null,
)

data class MyVerification(
    val id: Long = 0,
    val status: String = "",
    val reject_reason: String? = null,
    val real_name: String = "",
    val student_no: String = "",
    val created_at: String = "",
)

/** ---- 响应包装 ---- */

data class ApiErrorPayload(val code: String? = null, val message: String? = null)
data class ApiErrorBody(val error: ApiErrorPayload? = null)

data class AccountResp(val account: Account)

data class SmsCodeResp(
    val sent: Boolean = false,
    val dev_mode: Boolean = false,
    val dev_code: String? = null,
    val expires_in: Int = 0,
)

data class PostResp(val post: Post)

data class CreatePostResp(val post: Post, val moderation: ModerationInfo, val message: String = "")

data class CommentsResp(val items: List<CommentItem> = emptyList())

data class CreateCommentResp(val comment: CommentItem, val moderation: ModerationInfo, val message: String = "")

data class VerificationResp(val verification: MyVerification? = null)

data class ChangePasswordResp(val changed: Boolean = false)

data class NotificationsResp(val items: List<AppNotification> = emptyList(), val unread: Int = 0)

data class UnreadResp(val unread: Int = 0)

data class DirectConversationDetail(
    val id: Long = 0,
    val messages: List<DirectMessage> = emptyList(),
    val updated_at: String = "",
    val unread_count: Int = 0,
)

data class DirectConversationResp(
    val conversation: DirectConversationDetail,
    val other: Account,
    val unlocked: Boolean = false,
)

data class SendDirectMessageResp(val message: DirectMessage, val unlocked: Boolean = false)

data class StartDirectConversationResp(val item: DirectConversationItem)

data class AIConversationsResp(val items: List<AIConversation> = emptyList(), val remaining: Int = 0)

data class AIConversationResp(val conversation: AIConversation)

data class AskAIResp(val user_message: AIMessage, val answer: AIMessage, val remaining: Int = 0)

/** AI 知识库答案反馈：satisfied=false 时 answer 为联网重答的新答案。 */
data class AIFeedbackResp(val answer: AIMessage? = null, val remaining: Int = 0)

data class UploadResp(val url: String = "", val dev_mode: Boolean = false)

data class ResetPasswordResp(val reset: Boolean = false)

/** 举报等仅返回提示语的响应。 */
data class MessageResp(val message: String = "")

data class UpdatePostResp(val post: Post, val message: String = "")

data class PublicUserResp(val user: PublicAccount, val posts: List<Post> = emptyList())

data class AnnouncementResp(val announcement: Announcement)

data class SubmitAppealResp(val submitted: Boolean = false)

/** 帖子分页响应（对齐后端 listPosts 的 total / has_more）。 */
data class PostsPage(val items: List<Post> = emptyList(), val total: Int = 0, val has_more: Boolean = false)
