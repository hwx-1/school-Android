package com.example.schoolandorid.net

import com.example.schoolandorid.config.AppConfig
import com.example.schoolandorid.model.ApiErrorBody
import com.example.schoolandorid.model.UploadResp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class ApiError(val status: Int, val code: String, message: String) : Exception(message)

/**
 * Cookie 会话罐（对齐鸿蒙端 Http.ets CookieJar）：
 * 服务端基于 Cookie 鉴权，登录后捕获 Set-Cookie，后续请求统一携带；
 * 非 GET 请求自动附加 X-CSRF-Token（取自 xsnbb_csrf Cookie）。
 */
object CookieJar {
    private val cookies = java.util.concurrent.ConcurrentHashMap<String, String>()

    @Synchronized
    fun absorb(setCookies: List<String>) {
        for (line in setCookies) {
            val pair = line.split(';')[0]
            val eq = pair.indexOf('=')
            if (eq > 0) {
                val name = pair.substring(0, eq).trim()
                val value = pair.substring(eq + 1).trim()
                if (value.isEmpty() || value == "deleted") {
                    cookies.remove(name)
                } else {
                    cookies[name] = value
                }
            }
        }
    }

    fun header(): String = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

    fun csrfToken(): String = cookies["xsnbb_csrf"] ?: ""

    @Synchronized
    fun clear() = cookies.clear()

    fun hasSession(): Boolean = cookies.isNotEmpty()
}

object Http {
    @PublishedApi
    internal val gson = Gson()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(AppConfig.REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(AppConfig.REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .writeTimeout(AppConfig.REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    private val uploadClient: OkHttpClient = client.newBuilder()
        .readTimeout(AppConfig.REQUEST_TIMEOUT_MS * 4, TimeUnit.MILLISECONDS)
        .build()

    private val streamClient: OkHttpClient = client.newBuilder()
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun buildRequest(method: String, path: String, body: Any?): Request {
        val builder = Request.Builder()
            .url("${AppConfig.API_BASE_URL}$path")
            .header("Accept", "application/json")

        val cookie = CookieJar.header()
        if (cookie.isNotEmpty()) builder.header("Cookie", cookie)

        if (method != "GET") {
            val csrf = CookieJar.csrfToken()
            if (csrf.isNotEmpty()) builder.header("X-CSRF-Token", csrf)
        }

        when (method) {
            "GET" -> builder.get()
            "DELETE" -> {
                if (body != null) {
                    builder.delete(gson.toJson(body).toRequestBody("application/json".toMediaType()))
                } else {
                    builder.delete()
                }
            }
            else -> {
                val bodyText = gson.toJson(body ?: emptyMap<String, Any>())
                builder.method(method, bodyText.toRequestBody("application/json".toMediaType()))
            }
        }
        return builder.build()
    }

    /** 通用 JSON 请求。T 为 null 时表示无响应体（204 / 空串）。 */
    suspend fun request(
        method: String,
        path: String,
        body: Any? = null,
    ): String = withContext(Dispatchers.IO) {
        try {
            client.newCall(buildRequest(method, path, body)).execute().use { response ->
                CookieJar.absorb(response.headers("Set-Cookie"))
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val payload = runCatching { gson.fromJson(text, ApiErrorBody::class.java) }.getOrNull()
                    throw ApiError(
                        response.code,
                        payload?.error?.code ?: "UNKNOWN",
                        payload?.error?.message ?: "请求失败，请稍后重试",
                    )
                }
                text
            }
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw ApiError(0, "NETWORK_ERROR", "暂时无法连接服务器，请检查网络后重试（${e.message}）")
        }
    }

    /**
     * SSE 流式请求：逐行读取 `data:` 事件并回调。
     * 回调运行在 IO 线程，调用方通过 withContext(Dispatchers.Main) 切回主线程更新 UI。
     */
    suspend fun stream(
        method: String,
        path: String,
        body: Any? = null,
        onEvent: suspend (String) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest(method, path, body).newBuilder()
                .header("Accept", "text/event-stream")
                .build()
            streamClient.newCall(request).execute().use { response ->
                CookieJar.absorb(response.headers("Set-Cookie"))
                if (!response.isSuccessful) {
                    val text = response.body?.string().orEmpty()
                    val payload = runCatching { gson.fromJson(text, ApiErrorBody::class.java) }.getOrNull()
                    throw ApiError(
                        response.code,
                        payload?.error?.code ?: "UNKNOWN",
                        payload?.error?.message ?: "请求失败，请稍后重试",
                    )
                }
                val source = response.body?.source()
                    ?: throw ApiError(0, "STREAM_EMPTY", "未收到流式响应")
                while (true) {
                    val line = source.readUtf8Line() ?: break
                    val trimmed = line.trim()
                    if (trimmed.startsWith("data:")) {
                        onEvent(trimmed.removePrefix("data:").trim())
                    }
                }
            }
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw ApiError(0, "NETWORK_ERROR", "暂时无法连接服务器，请检查网络后重试（${e.message}）")
        }
    }

    inline fun <reified T> parse(text: String): T {
        return gson.fromJson(text, object : TypeToken<T>() {}.type)
    }

    /** 图片上传：multipart/form-data。 */
    suspend fun uploadFile(file: File, fileName: String, contentType: String): UploadResp =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url("${AppConfig.API_BASE_URL}/api/v1/uploads")
            val cookie = CookieJar.header()
            if (cookie.isNotEmpty()) builder.header("Cookie", cookie)
            val csrf = CookieJar.csrfToken()
            if (csrf.isNotEmpty()) builder.header("X-CSRF-Token", csrf)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    file.asRequestBody(contentType.toMediaType()),
                )
                .build()
            builder.post(requestBody)

            try {
                uploadClient.newCall(builder.build()).execute().use { response ->
                    CookieJar.absorb(response.headers("Set-Cookie"))
                    val text = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val payload = runCatching { gson.fromJson(text, ApiErrorBody::class.java) }.getOrNull()
                        throw ApiError(
                            response.code,
                            payload?.error?.code ?: "UPLOAD_FAILED",
                            payload?.error?.message ?: "图片上传失败",
                        )
                    }
                    gson.fromJson(text, UploadResp::class.java)
                }
            } catch (e: ApiError) {
                throw e
            } catch (e: Exception) {
                throw ApiError(0, "NETWORK_ERROR", "图片上传失败，请检查网络后重试（${e.message}）")
            }
        }

    fun absoluteMediaUrl(path: String?): String {
        if (path.isNullOrEmpty()) return ""
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("file://")) {
            return path
        }
        return "${AppConfig.API_BASE_URL}${if (path.startsWith("/")) path else "/$path"}"
    }
}
