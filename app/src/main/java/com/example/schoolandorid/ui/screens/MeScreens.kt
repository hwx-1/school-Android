package com.example.schoolandorid.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolandorid.config.AppColors
import com.example.schoolandorid.config.AppConfig
import com.example.schoolandorid.model.MyVerification
import com.example.schoolandorid.nav.NavStack
import com.example.schoolandorid.nav.Route
import com.example.schoolandorid.net.Api
import com.example.schoolandorid.net.Http
import com.example.schoolandorid.state.AppState
import com.example.schoolandorid.state.NotificationSync
import com.example.schoolandorid.ui.components.AvatarView
import com.example.schoolandorid.ui.components.FormField
import com.example.schoolandorid.ui.components.LoadingView
import com.example.schoolandorid.ui.components.NavBar
import com.example.schoolandorid.ui.errorMessage
import com.example.schoolandorid.ui.toast
import com.example.schoolandorid.util.ImageUtils
import kotlinx.coroutines.launch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Color
import com.example.schoolandorid.model.Appeal
import com.example.schoolandorid.ui.components.EmptyState
import com.example.schoolandorid.util.TimeUtil
/** 编辑资料（对齐鸿蒙端 pages/EditProfile.ets）。 */
@Composable
fun EditProfileScreen(nav: NavStack) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var nickname by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var realName by remember { mutableStateOf("") }
    var studentNo by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf("") }
    var avatarPreview by remember { mutableStateOf("") }
    var avatarLocalFile by remember { mutableStateOf<java.io.File?>(null) }
    var avatarUploading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppState.account?.let { account ->
            nickname = account.nickname
            gender = account.gender
            realName = account.real_name ?: ""
            studentNo = account.student_no ?: ""
            className = account.class_name ?: ""
            avatar = account.avatar
            avatarPreview = account.avatar
        }
    }

    fun canSave() = nickname.trim().isNotEmpty() && !saving && !avatarUploading

    fun cropAndUploadAvatar(uri: android.net.Uri) {
        scope.launch {
            avatarUploading = true
            val fileName = "avatar_${System.currentTimeMillis()}.jpg"
            val cacheFile = java.io.File(context.cacheDir, fileName)
            try {
                if (!ImageUtils.createSquareAvatar(context, uri, cacheFile)) {
                    context.toast("头像处理失败，请重新选择")
                    return@launch
                }
                avatarLocalFile = cacheFile
                avatarPreview = "local"
                val resp = Http.uploadFile(cacheFile, fileName, "image/jpeg")
                avatar = resp.url
                context.toast("头像已裁剪，保存资料后生效")
            } catch (err: Throwable) {
                avatarPreview = avatar
                avatarLocalFile = null
                context.toast(err.errorMessage("头像处理失败，请重新选择"))
            } finally {
                avatarUploading = false
            }
        }
    }

    val pickAvatar = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { cropAndUploadAvatar(it) }
    }

    fun save() {
        if (saving) return
        if (avatarUploading) {
            context.toast("头像正在处理中，请稍候")
            return
        }
        if (nickname.trim().isEmpty()) {
            context.toast("昵称不能为空")
            return
        }
        saving = true
        scope.launch {
            try {
                val resp = Api.updateProfile(
                    nickname = nickname.trim(),
                    avatar = avatar,
                    gender = gender,
                    realName = realName.trim(),
                    studentNo = studentNo.trim(),
                    className = className.trim(),
                )
                AppState.applyAccount(resp.account)
                context.toast("资料已保存")
                nav.pop()
            } catch (err: Throwable) {
                context.toast(err.errorMessage("保存失败，请稍后重试"))
            } finally {
                saving = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(
            title = "编辑资料",
            showBack = false,
            leftText = "取消",
            onLeft = { nav.pop() },
            rightText = if (saving) "保存中…" else "保存",
            rightEnabled = canSave(),
            onRight = { save() },
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // 头像区
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.CARD_BG, RoundedCornerShape(14.dp))
                    .clickable(enabled = !avatarUploading) {
                        pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    .padding(vertical = 18.dp),
            ) {
                Box(modifier = Modifier.size(96.dp)) {
                    val local = avatarLocalFile
                    if (local != null) {
                        coil.compose.AsyncImage(
                            model = local,
                            contentDescription = "头像",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .size(88.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(AppColors.PRIMARY),
                        )
                    } else {
                        Box(modifier = Modifier.align(Alignment.Center)) {
                            AvatarView(avatar = avatarPreview, nickname = nickname, diameter = 88)
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AppColors.PRIMARY)
                            .border(2.dp, AppColors.CARD_BG, CircleShape),
                    ) {
                        Text("✎", fontSize = 15.sp, color = androidx.compose.ui.graphics.Color.White)
                    }
                }
                if (avatarUploading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            color = AppColors.PRIMARY,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Text("正在裁剪并上传…", fontSize = 12.sp, color = AppColors.TEXT_SECONDARY)
                    }
                } else {
                    Text("更换头像", fontSize = 14.sp, color = AppColors.PRIMARY)
                }
                Text(
                    "支持拍照或从相册选择，可在系统编辑页调整裁剪区域",
                    fontSize = 11.sp,
                    color = AppColors.TEXT_SECONDARY,
                )
            }

            FormField(label = "昵称", placeholder = "请输入昵称", value = nickname, onValueChange = { nickname = it })

            // 性别选择
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Text("性别", fontSize = 13.sp, color = AppColors.TEXT_SECONDARY)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("男", "女", "保密").forEach { option ->
                        val selected = gender == option
                        Text(
                            option,
                            fontSize = 14.sp,
                            color = if (selected) androidx.compose.ui.graphics.Color.White else AppColors.TEXT_PRIMARY,
                            modifier = Modifier
                                .background(
                                    if (selected) AppColors.PRIMARY else AppColors.CARD_BG,
                                    RoundedCornerShape(18.dp),
                                )
                                .clickable { gender = option }
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            FormField(label = "真实姓名", placeholder = "选填", value = realName, onValueChange = { realName = it })
            FormField(label = "学号", placeholder = "选填", value = studentNo, onValueChange = { studentNo = it })
            FormField(label = "班级", placeholder = "选填", value = className, onValueChange = { className = it })

            Text(
                "学号与班级将用于学生认证核验，请如实填写。",
                fontSize = 11.sp,
                color = AppColors.TEXT_SECONDARY,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 学生认证（对齐鸿蒙端 pages/Verification.ets）。 */
@Composable
fun VerificationScreen(nav: NavStack) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var current by remember { mutableStateOf<MyVerification?>(null) }
    var realName by remember { mutableStateOf("") }
    var studentNo by remember { mutableStateOf("") }
    var materialUrl by remember { mutableStateOf("") }
    var uploading by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val resp = Api.myVerification()
            current = resp.verification
            resp.verification?.let {
                realName = it.real_name
                studentNo = it.student_no
            }
        } catch (_: Throwable) {
        }
        loading = false
    }

    fun statusText(status: String): String = when (status) {
        "pending" -> "审核中"
        "approved" -> "已通过"
        "rejected" -> "已驳回"
        else -> status
    }

    fun statusColor(status: String) = when (status) {
        "pending" -> AppColors.ORANGE
        "approved" -> AppColors.SUCCESS
        "rejected" -> AppColors.DANGER
        else -> AppColors.TEXT_SECONDARY
    }

    fun pickMaterial(uri: android.net.Uri) {
        if (uploading) return
        scope.launch {
            val fileName = "verify_${System.currentTimeMillis()}.jpg"
            val cacheFile = ImageUtils.copyToCache(context, uri, fileName)
            if (cacheFile == null) {
                context.toast("材料上传失败")
                return@launch
            }
            uploading = true
            try {
                val resp = Http.uploadFile(cacheFile, fileName, "image/jpeg")
                materialUrl = resp.url
                context.toast("材料上传成功")
            } catch (err: Throwable) {
                context.toast(err.errorMessage("材料上传失败"))
            } finally {
                uploading = false
            }
        }
    }

    val pickMaterialLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { pickMaterial(it) }
    }

    fun canSubmit() = realName.trim().isNotEmpty() && studentNo.trim().isNotEmpty() &&
        materialUrl.isNotEmpty() && !submitting && !uploading

    fun submit() {
        if (!canSubmit()) return
        submitting = true
        scope.launch {
            try {
                val resp = Api.submitVerification(materialUrl, realName.trim(), studentNo.trim())
                current = resp.verification
                context.toast("认证材料已提交，等待管理员审核")
            } catch (err: Throwable) {
                context.toast(err.errorMessage("提交失败，请稍后重试"))
            } finally {
                submitting = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(title = "学生认证", onBack = { nav.pop() })

        if (loading) {
            LoadingView()
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                current?.let { verification ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.CARD_BG, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                    ) {
                        Text("当前状态", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY)
                        Text(
                            statusText(verification.status),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusColor(verification.status),
                        )
                    }
                    if (verification.status == "rejected" && !verification.reject_reason.isNullOrEmpty()) {
                        Text(
                            "驳回原因：${verification.reject_reason}",
                            fontSize = 12.sp,
                            color = AppColors.DANGER,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (current == null || current?.status == "rejected") {
                    FormField(label = "真实姓名", placeholder = "与学籍信息一致", value = realName, onValueChange = { realName = it })
                    FormField(label = "学号", placeholder = "请输入学号", value = studentNo, onValueChange = { studentNo = it })

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("认证材料（学生证 / 校园卡照片）", fontSize = 13.sp, color = AppColors.TEXT_SECONDARY)
                        Text(
                            if (materialUrl.isNotEmpty()) "已上传 ✓" else if (uploading) "上传中…" else "选择图片",
                            fontSize = 14.sp,
                            color = if (materialUrl.isNotEmpty()) AppColors.SUCCESS else AppColors.PRIMARY,
                            modifier = Modifier
                                .background(AppColors.CARD_BG, RoundedCornerShape(10.dp))
                                .clickable(enabled = !uploading) {
                                    pickMaterialLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }

                    Button(
                        onClick = { submit() },
                        enabled = canSubmit(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.PRIMARY,
                            disabledContainerColor = AppColors.PRIMARY_DISABLED,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                    ) {
                        Text(if (submitting) "提交中…" else "提交认证", fontSize = 15.sp)
                    }
                } else {
                    Text(
                        "认证信息已在流程中，如需修改请联系管理员。",
                        fontSize = 12.sp,
                        color = AppColors.TEXT_SECONDARY,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** 账号设置（对齐鸿蒙端 pages/AccountSettings.ets）：修改密码 + 注销账号。 */
@Composable
fun AccountSettingsScreen(nav: NavStack) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var changing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    fun canChangePassword() = currentPassword.isNotEmpty() && newPassword.length >= 8 &&
        newPassword == confirmPassword && !changing

    fun changePassword() {
        if (!canChangePassword()) {
            if (newPassword != confirmPassword) {
                context.toast("两次输入的新密码不一致")
            }
            return
        }
        changing = true
        scope.launch {
            try {
                Api.changePassword(currentPassword, newPassword)
                context.toast("密码已修改")
                currentPassword = ""
                newPassword = ""
                confirmPassword = ""
            } catch (err: Throwable) {
                context.toast(err.errorMessage("修改失败，请稍后重试"))
            } finally {
                changing = false
            }
        }
    }

    fun deleteAccount() {
        if (deleting) return
        deleting = true
        scope.launch {
            try {
                Api.deleteAccount()
                AppState.logout()
                context.toast("账号已注销")
                nav.replace(Route.Login)
            } catch (err: Throwable) {
                context.toast(err.errorMessage("注销失败，请稍后重试"))
            } finally {
                deleting = false
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("注销账号") },
            text = { Text("注销后账号与数据将被删除且不可恢复，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    deleteAccount()
                }) {
                    Text("确认注销", color = AppColors.DANGER)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消", color = AppColors.TEXT_SECONDARY)
                }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(title = "账号设置", onBack = { nav.pop() })

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "修改密码",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TEXT_PRIMARY,
                    modifier = Modifier.fillMaxWidth(),
                )
                FormField(
                    label = "当前密码",
                    placeholder = "请输入当前密码",
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    isPassword = true,
                )
                FormField(
                    label = "新密码",
                    placeholder = "至少 8 位",
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    isPassword = true,
                )
                FormField(
                    label = "确认新密码",
                    placeholder = "再次输入新密码",
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    isPassword = true,
                )
                Button(
                    onClick = { changePassword() },
                    enabled = canChangePassword(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.PRIMARY,
                        disabledContainerColor = AppColors.PRIMARY_DISABLED,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                ) {
                    Text(if (changing) "提交中…" else "确认修改", fontSize = 14.sp)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "危险操作",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.DANGER,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "注销账号将删除账号、帖子、评论与私信记录，操作不可撤销。",
                    fontSize = 12.sp,
                    color = AppColors.TEXT_SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (deleting) "注销中…" else "注销账号",
                    fontSize = 14.sp,
                    color = AppColors.DANGER,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.DANGER_BG, RoundedCornerShape(10.dp))
                        .clickable { showDeleteDialog = true }
                        .padding(12.dp),
                )
            }

            Text(
                "接口地址：${AppConfig.API_BASE_URL}",
                fontSize = 11.sp,
                color = AppColors.TEXT_SECONDARY,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 消息通知设置（对齐鸿蒙端 pages/NotificationSettings.ets），偏好持久化到本机。 */
@Composable
fun NotificationSettingsScreen(nav: NavStack) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(title = "消息通知设置", onBack = { nav.pop() })

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth().background(AppColors.CARD_BG, RoundedCornerShape(14.dp))) {
                SwitchRow(
                    title = "应用内新消息提醒",
                    desc = "前台使用时以轻提示告知新的点赞、评论和系统消息",
                    enabled = AppState.inAppNotificationEnabled,
                    onChange = { AppState.inAppNotificationEnabled = it },
                )
                HorizontalDivider(color = AppColors.DIVIDER, modifier = Modifier.padding(horizontal = 16.dp))
                SwitchRow(
                    title = "底栏未读气泡",
                    desc = "在“消息”图标右上角显示未读数量",
                    enabled = AppState.notificationBadgeEnabled,
                    onChange = { AppState.notificationBadgeEnabled = it },
                )
                HorizontalDivider(color = AppColors.DIVIDER, modifier = Modifier.padding(horizontal = 16.dp))
                SwitchRow(
                    title = "自动同步消息",
                    desc = "应用处于前台时定期拉取新消息；关闭后进入消息页仍可刷新",
                    enabled = AppState.notificationAutoSyncEnabled,
                    onChange = { enabled ->
                        AppState.notificationAutoSyncEnabled = enabled
                    },
                )
            }

            Text(
                "以上设置只影响当前设备，不会删除消息。系统级通知权限仍可在设备“设置”中管理。",
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = AppColors.TEXT_SECONDARY,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = AppColors.TEXT_PRIMARY)
            Text(desc, fontSize = 11.sp, lineHeight = 17.sp, color = AppColors.TEXT_SECONDARY)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { value ->
                onChange(value)
                if (title == "自动同步消息" && value) {
                    scope.launch { NotificationSync.syncNow() }
                }
            },
            colors = SwitchDefaults.colors(checkedTrackColor = AppColors.PRIMARY),
        )
    }
}

/** 关于页（对齐鸿蒙端 pages/About.ets）。 */
@Composable
fun AboutScreen(nav: NavStack) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(title = "关于沈大社区", onBack = { nav.pop() })

        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 20.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(AppColors.PRIMARY),
                ) {
                    Text("沈", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                }
                Text("沈大社区", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.TEXT_PRIMARY)
                Text(
                    "版本 ${AppConfig.APP_VERSION}（Build ${AppConfig.BUILD_VERSION}）",
                    fontSize = 12.sp,
                    color = AppColors.TEXT_SECONDARY,
                )
                Text(
                    "面向沈阳大学校园成员的信息交流与校园服务平台。",
                    fontSize = 13.sp,
                    color = AppColors.TEXT_SECONDARY,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            Column(modifier = Modifier.fillMaxWidth().background(AppColors.CARD_BG, RoundedCornerShape(14.dp))) {
                InfoRow("当前版本", AppConfig.APP_VERSION)
                HorizontalDivider(color = AppColors.DIVIDER, modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow("构建版本", AppConfig.BUILD_VERSION)
                HorizontalDivider(color = AppColors.DIVIDER, modifier = Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.push(Route.LegalDocument("privacy")) }
                        .padding(16.dp),
                ) {
                    Text("隐私政策", fontSize = 15.sp, color = AppColors.TEXT_PRIMARY)
                    Spacer(Modifier.weight(1f))
                    Text("›", fontSize = 18.sp, color = AppColors.TEXT_SECONDARY)
                }
            }

            Text(
                "Copyright © 2026 沈大社区",
                fontSize = 11.sp,
                color = AppColors.TEXT_SECONDARY,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(label, fontSize = 15.sp, color = AppColors.TEXT_PRIMARY)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = AppColors.TEXT_SECONDARY)
    }
}

// ---- 法律文档（对齐鸿蒙端 pages/LegalDocument.ets）----

private data class LegalSection(val title: String, val body: String)

private val PRIVACY_SECTIONS = listOf(
    LegalSection(
        "一、我们收集的信息",
        "为了完成注册、登录和账号安全验证，我们会处理手机号、昵称及登录凭证。你主动进行学生认证时，我们会处理姓名、学号、班级和认证材料；你发布内容时，我们会处理帖子、评论、图片、收藏和互动记录。为了保障服务安全，还可能记录必要的网络请求、操作时间和异常日志。",
    ),
    LegalSection(
        "二、设备权限与图片",
        "只有在你主动选择头像、发布图片或拍摄照片时，应用才会调用系统相册、图片编辑或相机能力。你可以取消选择或在系统设置中管理权限；拒绝相关权限不会影响浏览、点赞等基础功能。",
    ),
    LegalSection(
        "三、信息使用目的",
        "相关信息用于提供校园社区服务、展示个人资料、完成学生身份核验、同步点赞评论消息、处理举报申诉、保障账号与内容安全，以及改进产品体验。我们不会将个人信息出售给第三方。",
    ),
    LegalSection(
        "四、信息共享与披露",
        "除获得你的单独同意、为实现你主动使用的服务所必需，或法律法规、司法及行政机关依法要求外，我们不会向无关第三方提供你的个人信息。公开发布的昵称、头像、帖子和评论会被其他社区用户看到。",
    ),
    LegalSection(
        "五、保存与安全",
        "我们仅在实现服务目的所需期限内保存信息，并采取访问控制、权限隔离和传输保护等措施。请勿在帖子或评论中公开身份证号、银行卡号、住址等敏感信息。",
    ),
    LegalSection(
        "六、你的权利",
        "你可以在“我的”页面查看和修改个人资料、管理消息提醒、查看收藏，并可在“账号设置”中修改密码或申请注销账号。注销后将按服务规则处理账号及相关数据，法律法规要求保留的除外。",
    ),
    LegalSection(
        "七、未成年人保护",
        "若你未满十八周岁，应在监护人指导下阅读并同意本政策。我们不会以诱导方式收集与校园社区服务无关的未成年人信息。",
    ),
    LegalSection(
        "八、政策更新与联系",
        "政策发生重要变化时，我们会通过应用内公告或显著提示告知。对个人信息处理有疑问或需要行使相关权利，可通过校园社区运营反馈渠道联系我们。",
    ),
)

private val AGREEMENT_SECTIONS = listOf(
    LegalSection(
        "一、服务范围",
        "沈大社区为校园成员提供信息交流、内容发布、校园工具和消息互动等服务。部分功能可能要求完成手机号注册、邀请码验证或学生身份认证。",
    ),
    LegalSection(
        "二、账号使用",
        "你应提供真实、准确、合法的信息并妥善保管账号凭证。不得转让、出借账号，不得冒用他人身份或利用账号从事违法违规活动。",
    ),
    LegalSection(
        "三、社区内容规范",
        "不得发布违法、侵权、欺诈、骚扰、色情、暴力、恶意营销或泄露他人隐私的内容。平台可依据社区规则对违规内容采取限制展示、删除、禁言或封禁等措施，并提供举报和申诉渠道。",
    ),
    LegalSection(
        "四、用户内容与责任",
        "你应确保发布内容拥有合法权利，并对内容真实性和合法性负责。引用他人作品时应尊重著作权、署名权及其他合法权益。",
    ),
    LegalSection(
        "五、服务调整",
        "我们可能根据运营、安全或合规要求调整功能。发生重大变更、暂停或终止服务时，将尽可能通过应用内公告提前说明。",
    ),
    LegalSection(
        "六、协议更新",
        "本协议更新后会展示新的生效日期。继续使用相关服务前，我们可能再次征求你的确认。",
    ),
)

/** 用户协议与隐私政策的统一阅读页。 */
@Composable
fun LegalDocumentScreen(nav: NavStack, documentType: String) {
    val isPrivacy = documentType != "agreement"
    val title = if (isPrivacy) "隐私政策" else "用户协议"
    val sections = if (isPrivacy) PRIVACY_SECTIONS else AGREEMENT_SECTIONS

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(title = title, onBack = { nav.pop() })

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 30.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.TEXT_PRIMARY)
                Text(
                    "更新日期：2026年8月30日 · 生效日期：2026年8月30日",
                    fontSize = 12.sp,
                    color = AppColors.TEXT_SECONDARY,
                )
            }

            Text(
                if (isPrivacy) {
                    "沈大社区重视并保护你的个人信息。请在使用服务前仔细阅读本政策，尤其是涉及学生认证材料、图片权限和账号注销的内容。"
                } else {
                    "欢迎使用沈大社区。请在注册或使用服务前仔细阅读本协议；完成勾选并继续使用，即表示你理解并同意相关约定。"
                },
                fontSize = 14.sp,
                lineHeight = 23.sp,
                color = AppColors.TEXT_SECONDARY,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.LEGAL_TIP_BG, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            )

            sections.forEach { section ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.CARD_BG, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                ) {
                    Text(
                        section.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TEXT_PRIMARY,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        section.body,
                        fontSize = 14.sp,
                        lineHeight = 23.sp,
                        color = AppColors.TEXT_SECONDARY,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** 我的申诉（对齐鸿蒙端 pages/Appeals.ets）：查看申诉记录，punishmentId > 0 时可直接发起该处罚的申诉。 */
@Composable
fun AppealsScreen(nav: NavStack, punishmentId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var appeals by remember { mutableStateOf<List<Appeal>>(emptyList()) }
    var reason by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    suspend fun load() {
        try {
            appeals = Api.myAppeals().sortedByDescending { it.created_at }
        } catch (_: Throwable) {
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    fun statusText(status: String) = when (status) {
        "pending" -> "待处理"
        "lifted" -> "处罚已解除"
        "upheld" -> "维持处罚"
        else -> status
    }

    fun statusColor(status: String) = when (status) {
        "pending" -> AppColors.ORANGE
        "lifted" -> AppColors.SUCCESS
        "upheld" -> AppColors.DANGER
        else -> AppColors.TEXT_SECONDARY
    }

    fun submit() {
        if (reason.trim().isEmpty() || submitting) return
        submitting = true
        scope.launch {
            try {
                Api.createAppeal(punishmentId, reason.trim())
                reason = ""
                context.toast("申诉已提交，等待管理员处理")
                load()
            } catch (err: Throwable) {
                context.toast(err.errorMessage("提交失败，请稍后重试"))
            } finally {
                submitting = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PAGE_BG)) {
        NavBar(title = "我的申诉", onBack = { nav.pop() })

        if (loading) {
            LoadingView()
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                if (punishmentId > 0) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.CARD_BG, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                    ) {
                        Text("发起申诉", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColors.TEXT_PRIMARY)
                        Text("请说明申诉理由，管理员核实后会尽快处理。", fontSize = 12.sp, color = AppColors.TEXT_SECONDARY)
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            placeholder = { Text("请填写申诉理由", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY) },
                            minLines = 4,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = AppColors.PAGE_BG,
                                focusedContainerColor = AppColors.CARD_BG,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = AppColors.PRIMARY,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { submit() },
                            enabled = reason.trim().isNotEmpty() && !submitting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.PRIMARY,
                                disabledContainerColor = AppColors.PRIMARY_DISABLED,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                        ) {
                            Text(if (submitting) "提交中…" else "提交申诉", fontSize = 14.sp)
                        }
                    }
                }

                if (appeals.isEmpty()) {
                    EmptyState(title = "暂无申诉记录", desc = "收到处罚通知后，可在此提交申诉。")
                } else {
                    Text("申诉记录", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColors.TEXT_PRIMARY, modifier = Modifier.fillMaxWidth())
                    appeals.forEach { appeal ->
                        val color = statusColor(appeal.status)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.CARD_BG, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (appeal.kind == "ban") "账号封禁申诉" else "禁言申诉", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.TEXT_PRIMARY)
                                Text(
                                    statusText(appeal.status),
                                    fontSize = 12.sp,
                                    color = color,
                                    modifier = Modifier
                                        .background(color.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                            Text("申诉理由：${appeal.reason}", fontSize = 13.sp, lineHeight = 20.sp, color = AppColors.TEXT_PRIMARY, modifier = Modifier.fillMaxWidth())
                            Text("提交于 ${TimeUtil.format(appeal.created_at)}", fontSize = 11.sp, color = AppColors.TEXT_SECONDARY, modifier = Modifier.fillMaxWidth())
                            if (!appeal.result.isNullOrEmpty()) {
                                Text("处理结果：${appeal.result}", fontSize = 12.sp, color = AppColors.TEXT_SECONDARY, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}