package com.example.schoolandorid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.schoolandorid.config.AppColors
import com.example.schoolandorid.model.Announcement
import com.example.schoolandorid.model.AppNotification
import com.example.schoolandorid.model.CommentItem
import com.example.schoolandorid.model.DirectConversationItem
import com.example.schoolandorid.model.DirectMessage
import com.example.schoolandorid.model.Post
import com.example.schoolandorid.net.Http
import com.example.schoolandorid.state.AppState
import com.example.schoolandorid.util.TimeUtil

/**
 * 信息流帖子卡片（对齐鸿蒙端 PostCard.ets，知乎内容页风格）：
 * 扁平白色卡片、赞同/评论/收藏文字操作栏。
 */
@Composable
fun PostCard(
    post: Post,
    onOpen: (Post) -> Unit = {},
    onLike: (Post) -> Unit = {},
    onBookmark: (Post) -> Unit = {},
    onTag: (String) -> Unit = {},
) {
    val imageUrls = post.images.orEmpty().map { Http.absoluteMediaUrl(it) }
    val imageSpan = when {
        imageUrls.size == 1 -> 1
        imageUrls.size == 2 || imageUrls.size == 4 -> 2
        else -> 3
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.CARD_BG)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 作者行
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.clickable { onOpen(post) },
        ) {
            AvatarView(avatar = post.author.avatar, nickname = post.author.nickname, diameter = 40)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        post.author.nickname,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TEXT_PRIMARY,
                    )
                    if (post.author.verified) {
                        BadgeView(badge = post.author.badge ?: "org")
                    }
                    if (post.pinned) {
                        Text(
                            "置顶",
                            fontSize = 10.sp,
                            color = AppColors.ORANGE,
                            modifier = Modifier
                                .border(1.dp, AppColors.ORANGE, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(TimeUtil.format(post.created_at), fontSize = 12.sp, color = AppColors.TEXT_SECONDARY)
            }
        }

        // 正文
        Text(
            post.text,
            fontSize = 15.sp,
            color = AppColors.TEXT_PRIMARY,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { onOpen(post) },
        )

        // 图片九宫格
        if (imageUrls.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onOpen(post) },
            ) {
                imageUrls.chunked(imageSpan).forEach { rowUrls ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowUrls.forEach { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.ICON_BG),
                            )
                        }
                        repeat(imageSpan - rowUrls.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 标签
        if (!post.tags.isNullOrEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                post.tags.take(3).forEach { tag ->
                    TagChip(text = tag, onTap = onTag)
                }
            }
        }

        // 操作栏：赞同 / 评论 / 收藏
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PostActionButton(
                modifier = Modifier.weight(1f),
                active = post.liked,
                activeColor = AppColors.PRIMARY,
                activeBg = AppColors.LIKE_BG,
                count = post.likes,
                icon = {
                    Icon(
                        if (post.liked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "赞同",
                        tint = if (post.liked) AppColors.PRIMARY else AppColors.TEXT_SECONDARY,
                        modifier = Modifier.size(16.dp),
                    )
                },
                onClick = { onLike(post) },
            )
            PostActionButton(
                modifier = Modifier.weight(1f),
                active = false,
                activeColor = AppColors.TEXT_SECONDARY,
                activeBg = AppColors.ACTION_BG,
                count = post.comments,
                icon = {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "评论",
                        tint = AppColors.TEXT_SECONDARY,
                        modifier = Modifier.size(16.dp),
                    )
                },
                onClick = { onOpen(post) },
            )
            PostActionButton(
                modifier = Modifier.weight(1f),
                active = post.bookmarked,
                activeColor = AppColors.ORANGE,
                activeBg = AppColors.ORANGE_BG,
                count = post.bookmarks,
                icon = {
                    Icon(
                        if (post.bookmarked) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = "收藏",
                        tint = if (post.bookmarked) AppColors.ORANGE else AppColors.TEXT_SECONDARY,
                        modifier = Modifier.size(16.dp),
                    )
                },
                onClick = { onBookmark(post) },
            )
        }
    }
}

@Composable
private fun PostActionButton(
    modifier: Modifier,
    active: Boolean,
    activeColor: Color,
    activeBg: Color,
    count: Int,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(if (active) activeBg else AppColors.ACTION_BG)
            .clickable { onClick() },
    ) {
        icon()
        Spacer(Modifier.width(5.dp))
        Text(
            "$count",
            fontSize = 13.sp,
            color = if (active) activeColor else AppColors.TEXT_SECONDARY,
            maxLines = 1,
        )
    }
}

/** 评论条目（抖音式两级评论，对齐鸿蒙端 CommentItemView.ets）。 */
@Composable
fun CommentItemView(
    comment: CommentItem,
    replyToName: String = "",
    isReply: Boolean = false,
    onReply: (CommentItem) -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !comment.deleted) { onReply(comment) }
            .padding(vertical = 10.dp),
    ) {
        AvatarView(
            avatar = comment.author.avatar,
            nickname = comment.author.nickname,
            diameter = if (isReply) 24 else 32,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (comment.deleted) "用户" else comment.author.nickname,
                    fontSize = 13.sp,
                    color = AppColors.TEXT_SECONDARY,
                )
                if (!comment.deleted && replyToName.isNotEmpty()) {
                    Text("回复 $replyToName", fontSize = 13.sp, color = AppColors.TEXT_SECONDARY)
                }
                if (!comment.deleted && comment.author.verified) {
                    BadgeView(badge = comment.author.badge ?: "org")
                }
            }
            Text(
                if (comment.deleted) "该评论已删除" else comment.text,
                fontSize = 14.sp,
                color = if (comment.deleted) AppColors.TEXT_SECONDARY else AppColors.TEXT_PRIMARY,
                fontStyle = if (comment.deleted) FontStyle.Italic else FontStyle.Normal,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(TimeUtil.format(comment.created_at), fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
                if (!comment.deleted) {
                    Text("回复", fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
                }
            }
        }
    }
}

/** 私信会话条目（对齐鸿蒙端 ConversationItemView.ets）。
 *  未读数冒泡位于卡片右侧、时间下方，直接读取服务端会话快照的 unread_count。 */
@Composable
fun ConversationItemView(
    conversation: DirectConversationItem,
    onOpen: (DirectConversationItem) -> Unit = {},
) {
    val lastMessage = run {
        val last = conversation.messages.lastOrNull()
        when {
            last == null -> "开始聊天吧"
            else -> {
                val mine = last.sender_id == (AppState.account?.id ?: -1L)
                val text = if (last.system) "[系统招呼]" else last.text
                if (mine) "我：$text" else text
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.CARD_BG, RoundedCornerShape(12.dp))
            .clickable { onOpen(conversation) }
            .padding(16.dp),
    ) {
        AvatarView(
            avatar = conversation.other.avatar,
            nickname = conversation.other.nickname,
            diameter = 46,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                conversation.other.nickname,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TEXT_PRIMARY,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    lastMessage,
                    fontSize = 13.sp,
                    color = AppColors.TEXT_SECONDARY,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (conversation.unlocked) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "自由聊",
                        fontSize = 10.sp,
                        color = AppColors.SUCCESS,
                        modifier = Modifier
                            .background(AppColors.SUCCESS_BG, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(TimeUtil.format(conversation.updated_at), fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
            if (conversation.unread_count > 0) {
                UnreadBadge(count = conversation.unread_count)
            }
        }
    }
}

/** 未读数冒泡。 */
@Composable
fun UnreadBadge(count: Int) {
    Text(
        if (count > 99) "99+" else "$count",
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
            .height(16.dp)
            .widthIn(min = 16.dp)
            .background(AppColors.DANGER, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp),
    )
}

/** 聊天气泡：区分我 / 对方 / 系统消息（对齐鸿蒙端 MessageBubble.ets）。 */
@Composable
fun MessageBubble(message: DirectMessage, isMine: Boolean) {
    if (message.system) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            Text(
                message.text.ifEmpty { "👋 对方发来一个系统招呼" },
                fontSize = 12.sp,
                color = AppColors.TEXT_SECONDARY,
                modifier = Modifier
                    .background(Color(0x0D000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    } else {
        Row(
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            Text(
                message.text,
                fontSize = 15.sp,
                color = if (isMine) Color.White else AppColors.TEXT_PRIMARY,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        if (isMine) AppColors.PRIMARY else AppColors.CARD_BG,
                        RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

/** 系统通知条目（对齐鸿蒙端 NotificationItemView.ets）。 */
@Composable
fun NotificationItemView(
    notification: AppNotification,
    onOpen: (AppNotification) -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.CARD_BG, RoundedCornerShape(12.dp))
            .clickable { onOpen(notification) }
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(AppColors.ICON_BG),
        ) {
            Text(
                notificationTypeIcon(notification.type),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = notificationTypeColor(notification.type),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.title,
                    fontSize = 15.sp,
                    fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Medium,
                    color = AppColors.TEXT_PRIMARY,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!notification.read) {
                    Spacer(Modifier.width(6.dp))
                    UnreadBadge(count = 1)
                }
            }
            if (notification.body.isNotEmpty()) {
                Text(
                    notification.body,
                    fontSize = 13.sp,
                    color = AppColors.TEXT_SECONDARY,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    notificationTypeLabel(notification.type),
                    fontSize = 11.sp,
                    color = notificationTypeColor(notification.type),
                )
                Text("·", fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
                Text(TimeUtil.format(notification.created_at), fontSize = 11.sp, color = AppColors.TEXT_SECONDARY)
            }
        }
    }
}

fun notificationTypeLabel(type: String): String = when (type) {
    "comment" -> "评论"
    "reply" -> "回复"
    "like" -> "赞同"
    "report_result" -> "举报结果"
    "official_answer" -> "官方答复"
    "punishment" -> "处罚通知"
    "appeal_result" -> "申诉结果"
    else -> "通知"
}

fun notificationTypeIcon(type: String): String = when (type) {
    "like" -> "♥"
    "comment", "reply" -> "●"
    "punishment", "appeal_result" -> "!"
    else -> "✦"
}

fun notificationTypeColor(type: String): Color = when (type) {
    "like" -> AppColors.DANGER
    "comment", "reply" -> AppColors.PRIMARY
    "punishment" -> AppColors.ORANGE
    else -> AppColors.PURPLE
}

/** 公告卡片，点击展开全文（对齐鸿蒙端 AnnouncementCard.ets）。 */
@Composable
fun AnnouncementCard(announcement: Announcement) {
    var expanded by remember(announcement.id) { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.CARD_BG, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📢", fontSize = 14.sp)
            Text(
                announcement.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TEXT_PRIMARY,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            if (expanded) announcement.body else announcement.summary,
            fontSize = 13.sp,
            color = AppColors.TEXT_SECONDARY,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row {
            Text(
                TimeUtil.format(announcement.created_at),
                fontSize = 11.sp,
                color = AppColors.TEXT_SECONDARY,
                modifier = Modifier.weight(1f),
            )
            Text(if (expanded) "收起" else "展开", fontSize = 12.sp, color = AppColors.PRIMARY)
        }
    }
}
