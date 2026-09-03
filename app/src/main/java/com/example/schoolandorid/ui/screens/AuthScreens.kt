package com.example.schoolandorid.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolandorid.config.AppColors
import com.example.schoolandorid.nav.NavStack
import com.example.schoolandorid.nav.Route
import com.example.schoolandorid.net.Api
import com.example.schoolandorid.state.AppState
import com.example.schoolandorid.state.NotificationSync
import com.example.schoolandorid.ui.components.LegalConsent
import com.example.schoolandorid.ui.errorMessage
import com.example.schoolandorid.ui.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = AppColors.CARD_BG,
    focusedContainerColor = AppColors.CARD_BG,
    unfocusedBorderColor = Color.Transparent,
    focusedBorderColor = AppColors.PRIMARY,
)

/** 登录页（对齐鸿蒙端 pages/Login.ets）。 */
@Composable
fun LoginScreen(nav: NavStack) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val agreed = AppState.privacyAgreed

    fun formReady() = phone.length == 11 && password.length >= 6

    fun handleLogin() {
        if (submitting) return
        if (phone.length != 11) {
            context.toast("请输入 11 位手机号")
            return
        }
        if (password.length < 6) {
            context.toast("密码至少 6 位")
            return
        }
        if (!agreed) {
            context.toast("请先阅读并同意《用户协议》和《隐私政策》")
            return
        }
        submitting = true
        scope.launch {
            try {
                val resp = Api.login(phone, password)
                AppState.applyAccount(resp.account)
                NotificationSync.syncNow()
                context.toast("欢迎回来，${resp.account.nickname}")
                nav.replace(Route.Tabs)
            } catch (err: Throwable) {
                context.toast(err.errorMessage("登录失败，请稍后重试"))
            } finally {
                submitting = false
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.PAGE_BG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
    ) {
        // Logo 区
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 72.dp, bottom = 40.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(listOf(AppColors.PRIMARY, AppColors.PRIMARY_LIGHT)),
                    ),
            ) {
                Text("沈", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text(
                "沈大社区",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TEXT_PRIMARY,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                "沈阳大学校园社区",
                fontSize = 13.sp,
                color = AppColors.TEXT_SECONDARY,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        // 输入区
        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.length <= 11 && it.all(Char::isDigit)) phone = it },
            placeholder = { Text("手机号", color = AppColors.TEXT_SECONDARY) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = authFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("密码（至少 6 位）", color = AppColors.TEXT_SECONDARY) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = authFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        )
        Spacer(Modifier.height(18.dp))

        LegalConsent(
            agreed = agreed,
            onAgreeChange = { AppState.privacyAgreed = it },
            onOpenAgreement = { nav.push(Route.LegalDocument("agreement")) },
            onOpenPrivacy = { nav.push(Route.LegalDocument("privacy")) },
        )

        // 登录按钮
        Button(
            onClick = { handleLogin() },
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (formReady() && agreed) AppColors.PRIMARY else AppColors.PRIMARY_DISABLED,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp)
                .height(50.dp),
        ) {
            if (submitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (submitting) "登录中…" else "登 录", fontSize = 17.sp, color = Color.White)
        }

        Row(modifier = Modifier.padding(top = 22.dp, bottom = 40.dp)) {
            Text("还没有账号？", fontSize = 13.sp, color = AppColors.TEXT_SECONDARY)
            Spacer(Modifier.width(4.dp))
            Text(
                "立即注册",
                fontSize = 13.sp,
                color = AppColors.PRIMARY,
                modifier = Modifier.clickable { nav.push(Route.Register) },
            )
            Spacer(Modifier.weight(1f))
            Text(
                "忘记密码？",
                fontSize = 13.sp,
                color = AppColors.PRIMARY,
                modifier = Modifier.clickable { nav.push(Route.ForgotPassword) },
            )
        }
    }
}

/** 忘记密码（对齐 web 端 AuthPage forgot 模式）：验证码重置密码。 */
@Composable
fun ForgotPasswordScreen(nav: NavStack) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var devCodeTip by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }

    fun canSendCode() = phone.length == 11 && !sending && countdown == 0
    fun canSubmit() = phone.length == 11 && code.length >= 4 && password.length >= 6 && !submitting

    fun handleSendCode() {
        if (!canSendCode()) return
        sending = true
        scope.launch {
            try {
                // purpose=reset：验证码用于重置密码，与注册验证码分开校验
                val resp = Api.smsCode(phone, purpose = "reset")
                launch {
                    countdown = 60
                    while (countdown > 0) {
                        delay(1000)
                        countdown -= 1
                    }
                }
                devCodeTip = if (resp.dev_mode && !resp.dev_code.isNullOrEmpty()) {
                    "开发模式验证码：${resp.dev_code}"
                } else {
                    "验证码已发送，请注意查收"
                }
                context.toast("验证码已下发")
            } catch (err: Throwable) {
                context.toast(err.errorMessage("发送失败，请稍后重试"))
            } finally {
                sending = false
            }
        }
    }

    fun handleReset() {
        if (!canSubmit()) {
            context.toast("请完整填写信息")
            return
        }
        submitting = true
        scope.launch {
            try {
                Api.resetPassword(phone, code, password)
                finished = true
            } catch (err: Throwable) {
                context.toast(err.errorMessage("重置失败，请稍后重试"))
            } finally {
                submitting = false
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.PAGE_BG)
            .verticalScroll(rememberScrollState())
            .padding(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 32.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "‹",
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TEXT_PRIMARY,
                modifier = Modifier.clickable { nav.pop() },
            )
        }

        if (finished) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
            ) {
                Text("✓", fontSize = 44.sp, color = AppColors.SUCCESS)
                Spacer(Modifier.height(12.dp))
                Text("密码重置成功", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = AppColors.TEXT_PRIMARY)
                Spacer(Modifier.height(8.dp))
                Text("请使用新密码重新登录社区。", fontSize = 13.sp, color = AppColors.TEXT_SECONDARY)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { nav.pop() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PRIMARY),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("返回登录", fontSize = 16.sp)
                }
            }
            return@Column
        }

        Text("重置登录密码", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.TEXT_PRIMARY)
        Text("验证码将发送至已绑定手机号。", fontSize = 13.sp, color = AppColors.TEXT_SECONDARY)

        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.length <= 11 && it.all(Char::isDigit)) phone = it },
            placeholder = { Text("手机号", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = authFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) code = it },
                placeholder = { Text("短信验证码", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = authFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            )
            Button(
                onClick = { handleSendCode() },
                enabled = canSendCode(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.PRIMARY,
                    disabledContainerColor = AppColors.PRIMARY_DISABLED,
                ),
                modifier = Modifier.height(52.dp),
            ) {
                Text(if (countdown > 0) "${countdown}s" else "获取验证码", fontSize = 13.sp)
            }
        }

        if (devCodeTip.isNotEmpty()) {
            Text(devCodeTip, fontSize = 12.sp, color = AppColors.ORANGE, modifier = Modifier.fillMaxWidth())
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("新密码（至少 6 位）", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = authFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )

        Button(
            onClick = { handleReset() },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canSubmit()) AppColors.PRIMARY else AppColors.PRIMARY_DISABLED,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(if (submitting) "提交中…" else "确认重置", fontSize = 16.sp)
        }
    }
}

/** 注册页（对齐鸿蒙端 pages/Register.ets）。 */
@Composable
fun RegisterScreen(nav: NavStack) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var devCodeTip by remember { mutableStateOf("") }
    val agreed = AppState.privacyAgreed

    fun canSendCode() = phone.length == 11 && !sending && countdown == 0
    fun canSubmit() = phone.length == 11 && code.length >= 4 && password.length >= 6 &&
        nickname.trim().isNotEmpty() && inviteCode.trim().isNotEmpty() && agreed && !submitting

    fun handleSendCode() {
        if (!canSendCode()) return
        sending = true
        scope.launch {
            try {
                val resp = Api.smsCode(phone)
                // 60s 倒计时
                launch {
                    countdown = 60
                    while (countdown > 0) {
                        delay(1000)
                        countdown -= 1
                    }
                }
                // 开发模式：真实短信未接入，验证码随响应下发并明确标注
                devCodeTip = if (resp.dev_mode && !resp.dev_code.isNullOrEmpty()) {
                    "开发模式验证码：${resp.dev_code}"
                } else {
                    "验证码已发送，请注意查收"
                }
                context.toast("验证码已下发")
            } catch (err: Throwable) {
                context.toast(err.errorMessage("发送失败，请稍后重试"))
            } finally {
                sending = false
            }
        }
    }

    fun handleRegister() {
        if (!agreed) {
            context.toast("请先阅读并同意《用户协议》和《隐私政策》")
            return
        }
        if (!canSubmit()) {
            context.toast("请完整填写注册信息")
            return
        }
        submitting = true
        scope.launch {
            try {
                val resp = Api.register(
                    phone = phone,
                    code = code,
                    password = password,
                    nickname = nickname.trim(),
                    inviteCode = inviteCode.trim(),
                )
                AppState.applyAccount(resp.account)
                NotificationSync.syncNow()
                context.toast("注册成功，欢迎加入沈大社区")
                nav.replace(Route.Tabs)
            } catch (err: Throwable) {
                context.toast(err.errorMessage("注册失败，请稍后重试"))
            } finally {
                submitting = false
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.PAGE_BG)
            .verticalScroll(rememberScrollState())
            .padding(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 32.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "‹",
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TEXT_PRIMARY,
                modifier = Modifier.clickable { nav.pop() },
            )
        }

        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.length <= 11 && it.all(Char::isDigit)) phone = it },
            placeholder = { Text("手机号", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = authFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) code = it },
                placeholder = { Text("短信验证码", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = authFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            )
            Button(
                onClick = { handleSendCode() },
                enabled = canSendCode(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.PRIMARY,
                    disabledContainerColor = AppColors.PRIMARY_DISABLED,
                ),
                modifier = Modifier.height(52.dp),
            ) {
                Text(if (countdown > 0) "${countdown}s" else "获取验证码", fontSize = 13.sp)
            }
        }

        if (devCodeTip.isNotEmpty()) {
            Text(devCodeTip, fontSize = 12.sp, color = AppColors.ORANGE, modifier = Modifier.fillMaxWidth())
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("密码（至少 6 位）", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = authFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )

        OutlinedTextField(
            value = nickname,
            onValueChange = { if (it.length <= 16) nickname = it },
            placeholder = { Text("昵称", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = authFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )

        OutlinedTextField(
            value = inviteCode,
            onValueChange = { inviteCode = it },
            placeholder = { Text("邀请码（内测默认 xsnbb-test）", fontSize = 14.sp, color = AppColors.TEXT_SECONDARY) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = authFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )

        LegalConsent(
            agreed = agreed,
            onAgreeChange = { AppState.privacyAgreed = it },
            onOpenAgreement = { nav.push(Route.LegalDocument("agreement")) },
            onOpenPrivacy = { nav.push(Route.LegalDocument("privacy")) },
        )

        Button(
            onClick = { handleRegister() },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canSubmit()) AppColors.PRIMARY else AppColors.PRIMARY_DISABLED,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(if (submitting) "注册中…" else "注册并登录", fontSize = 16.sp)
        }
    }
}
