package com.example.schoolandorid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.schoolandorid.config.AppColors
import com.example.schoolandorid.net.Http

/** 微信风格顶部导航栏（对齐鸿蒙端 NavBar.ets）：标题居中，左右可选文字/按钮操作。 */
@Composable
fun NavBar(
    title: String,
    subtitle: String = "",
    titleTag: String = "",
    showBack: Boolean = true,
    onBack: () -> Unit = {},
    leftText: String = "",
    onLeft: () -> Unit = {},
    rightText: String = "",
    rightFontSize: Int = 14,
    rightColor: Color = AppColors.PRIMARY,
    rightEnabled: Boolean = true,
    onRight: () -> Unit = {},
    rightButtonText: String = "",
    onRightButton: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 12.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
    ) {
        // 居中标题层
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 72.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TEXT_PRIMARY,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (titleTag.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        titleTag,
                        fontSize = 10.sp,
                        color = AppColors.SUCCESS,
                        modifier = Modifier
                            .background(AppColors.SUCCESS_BG, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = AppColors.TEXT_SECONDARY,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 左侧操作区
        Row(modifier = Modifier.align(Alignment.CenterStart)) {
            if (leftText.isNotEmpty()) {
                Text(
                    leftText,
                    fontSize = 14.sp,
                    color = AppColors.TEXT_SECONDARY,
                    modifier = Modifier
                        .clickable { onLeft() }
                        .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                )
            } else if (showBack) {
                Text(
                    "‹",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TEXT_PRIMARY,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                )
            }
        }

        // 右侧操作区
        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            if (rightButtonText.isNotEmpty()) {
                Button(
                    onClick = onRightButton,
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PRIMARY),
                ) {
                    Text(rightButtonText, fontSize = 13.sp)
                }
            } else if (rightText.isNotEmpty()) {
                Text(
                    rightText,
                    fontSize = rightFontSize.sp,
                    color = if (rightEnabled) rightColor else AppColors.TEXT_SECONDARY,
                    modifier = Modifier
                        .clickable(enabled = rightEnabled) { onRight() }
                        .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                )
            }
        }
    }
}

/** 居中加载指示。 */
@Composable
fun LoadingView(text: String = "加载中…") {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
    ) {
        CircularProgressIndicator(
            color = AppColors.PRIMARY,
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
        )
        Spacer(Modifier.height(12.dp))
        Text(text, fontSize = 13.sp, color = AppColors.TEXT_SECONDARY)
    }
}

/** 空态 / 错误态占位，带可选重试按钮。 */
@Composable
fun EmptyState(
    title: String = "暂无内容",
    desc: String = "",
    actionText: String = "",
    onAction: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp, horizontal = 32.dp),
    ) {
        Text("🍃", fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 16.sp, color = AppColors.TEXT_PRIMARY, fontWeight = FontWeight.Medium)
        if (desc.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(desc, fontSize = 13.sp, color = AppColors.TEXT_SECONDARY, textAlign = TextAlign.Center)
        }
        if (actionText.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.PRIMARY),
            ) {
                Text(actionText, fontSize = 14.sp)
            }
        }
    }
}

/** 表单输入行：标题 + 输入框。 */
@Composable
fun FormField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, color = AppColors.TEXT_SECONDARY)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = AppColors.TEXT_SECONDARY) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = AppColors.CARD_BG,
                focusedContainerColor = AppColors.CARD_BG,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = AppColors.PRIMARY,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )
    }
}

/** 登录与注册共用的协议确认组件。 */
@Composable
fun LegalConsent(
    agreed: Boolean,
    onAgreeChange: (Boolean) -> Unit,
    onOpenAgreement: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (agreed) AppColors.PRIMARY else Color.Transparent)
                .border(1.5.dp, if (agreed) AppColors.PRIMARY else Color(0xFFC4C9D4), CircleShape)
                .clickable { onAgreeChange(!agreed) },
        ) {
            if (agreed) {
                Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text("我已阅读并同意", fontSize = 12.sp, color = AppColors.TEXT_SECONDARY)
            Text(
                "《用户协议》",
                fontSize = 12.sp,
                color = AppColors.PRIMARY,
                modifier = Modifier.clickable { onOpenAgreement() },
            )
            Text("和", fontSize = 12.sp, color = AppColors.TEXT_SECONDARY)
            Text(
                "《隐私政策》",
                fontSize = 12.sp,
                color = AppColors.PRIMARY,
                modifier = Modifier.clickable { onOpenPrivacy() },
            )
        }
    }
}

/** 话题标签胶囊。 */
@Composable
fun TagChip(text: String, onTap: (String) -> Unit = {}) {
    Text(
        "# $text",
        fontSize = 12.sp,
        color = AppColors.PRIMARY,
        modifier = Modifier
            .background(AppColors.TAG_BG, RoundedCornerShape(12.dp))
            .clickable { onTap(text) }
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** 认证徽标：红=官方，蓝=认证机构，绿=管理员发布。 */
@Composable
fun BadgeView(badge: String = "org") {
    val color = when (badge) {
        "official" -> AppColors.DANGER
        "admin" -> AppColors.SUCCESS
        else -> AppColors.PRIMARY
    }
    val label = when (badge) {
        "official" -> "官方"
        "admin" -> "管理员"
        else -> "认证"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        ) {
            Text("✓", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 10.sp, color = color)
    }
}

/** 圆形头像：有图显示图，无图显示昵称首字。 */
@Composable
fun AvatarView(avatar: String, nickname: String, diameter: Int = 40) {
    val url = Http.absoluteMediaUrl(avatar)
    if (url.isNotEmpty()) {
        AsyncImage(
            model = url,
            contentDescription = nickname,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(diameter.dp)
                .clip(CircleShape)
                .background(AppColors.PRIMARY),
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(diameter.dp)
                .clip(CircleShape)
                .background(AppColors.PRIMARY),
        ) {
            Text(
                if (nickname.isNotEmpty()) nickname.substring(0, 1) else "客",
                fontSize = (diameter * 0.42).sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
