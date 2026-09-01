package com.example.schoolandorid.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val AVATAR_EDGE = 512

/** 图片工具（对齐鸿蒙端 utils/AvatarImage.ets 与 Compose.ets 的缓存复制逻辑）。 */
object ImageUtils {

    /** 将相册图片居中裁成正方形，并统一编码为 512px JPEG，写入 output。 */
    suspend fun createSquareAvatar(context: Context, sourceUri: Uri, output: File): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                } ?: return@withContext false
                val side = minOf(bitmap.width, bitmap.height)
                val x = (bitmap.width - side) / 2
                val y = (bitmap.height - side) / 2
                val cropped = Bitmap.createBitmap(bitmap, x, y, side, side)
                val scaled = Bitmap.createScaledBitmap(cropped, AVATAR_EDGE, AVATAR_EDGE, true)
                FileOutputStream(output).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                true
            } catch (_: Exception) {
                false
            }
        }

    /** 把 content Uri 复制进应用缓存目录，返回缓存文件；失败返回 null。 */
    suspend fun copyToCache(context: Context, sourceUri: Uri, fileName: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val target = File(context.cacheDir, fileName)
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                } ?: return@withContext null
                target
            } catch (_: Exception) {
                null
            }
        }
}
