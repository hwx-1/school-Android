package com.example.schoolandorid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolandorid.config.AppColors
import com.example.schoolandorid.net.Api
import com.example.schoolandorid.ui.errorMessage
import com.example.schoolandorid.ui.toast
import kotlinx.coroutines.launch

/** 举报原因分类（对齐 web 端 REPORT_REASONS）。 */
val REPORT_REASONS = listOf(
    "垃圾广告信息",
    "辱骂、歧视或恶意攻击",
    "淫秽色情或令人不适内容",
    "谣言或虚假信息",
    "违法犯罪或违规内容",
    "涉嫌侵权",
    "其他",
)

/** 举报目标：post / comment / user / conversation。 */
data class ReportTarget(val kind: String, val id: Long)

/**
 * 知乎式举报弹窗（对齐 web 端 ReportDialog）：
 * 单选原因 + 选填补充说明，提交后关闭并 toast 结果。
 */
@Composable
fun ReportDialog(target: ReportTarget, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reason by remember { mutableStateOf(REPORT_REASONS[0]) }
    var detail by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val kindLabel = when (target.kind) {
        "post" -> "帖子"
        "comment" -> "评论"
        "conversation" -> "私信"
        else -> "用户"
    }

    fun submit() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val fullReason = if (detail.trim().isNotEmpty()) "$reason：${detail.trim()}" else reason
                val resp = when (target.kind) {
                    "post" -> Api.reportPost(target.id, fullReason)
                    "comment" -> Api.reportComment(target.id, fullReason)
                    "conversation" -> Api.reportDirectConversation(target.id, fullReason)
                    else -> Api.reportUser(target.id, fullReason)
                }
                context.toast(resp.message.ifEmpty { "举报已提交，管理员会尽快处理" })
                onClose()
            } catch (err: Throwable) {
                context.toast(err.errorMessage("举报失败，请稍后重试"))
            } finally {
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        title = { Text("举报$kindLabel", fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "请选择最符合的原因，管理员不会向对方透露你的身份。",
                    fontSize = 12.sp,
                    color = AppColors.TEXT_SECONDARY,
                )
                REPORT_REASONS.forEach { item ->
                    val selected = item == reason
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) AppColors.PRIMARY_BG else Color.Transparent,
                                RoundedCornerShape(8.dp),
                            )
                            .border(
                                1.dp,
                                if (selected) AppColors.PRIMARY else AppColors.DIVIDER,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable { reason = item }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    ) {
                        Text(
                            item,
                            fontSize = 13.sp,
                            color = if (selected) AppColors.PRIMARY else AppColors.TEXT_PRIMARY,
                        )
                    }
                }
                OutlinedTextField(
                    value = detail,
                    onValueChange = { if (it.length <= 200) detail = it },
                    placeholder = {
                        Text("补充说明（选填，200 字以内）", fontSize = 13.sp, color = AppColors.TEXT_SECONDARY)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = AppColors.PAGE_BG,
                        focusedContainerColor = AppColors.PAGE_BG,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = AppColors.PRIMARY,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }, enabled = !busy) {
                if (busy) {
                    CircularProgressIndicator(
                        color = AppColors.PRIMARY,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .height(16.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text("提交举报", color = AppColors.PRIMARY)
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!busy) onClose() }) {
                Text("取消", color = AppColors.TEXT_SECONDARY)
            }
        },
    )
}
