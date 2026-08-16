/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * Ext.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.data.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.DisplayMetrics
import android.webkit.MimeTypeMap
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.my.kizzy.data.remote.ApiResponse
import com.my.kizzy.data.remote.ExternalAsset
import com.my.kizzy.data.remote.ImgurResponse
import com.my.kizzy.data.rpc.RpcImage
import com.my.kizzy.preference.Prefs
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import java.io.File
import java.io.FileOutputStream

suspend fun HttpResponse.toImageURL(): String? {
    return try {
        if (this.status == HttpStatusCode.OK)
            this.body<ImgurResponse>().data.link
        else
            null
    } catch (e: Exception) {
        null
    }
}

suspend fun HttpResponse.toExternalAsset(): String? {
    return try {
        if (this.status == HttpStatusCode.OK)
            "mp:" + this.body<Array<ExternalAsset>>().first().externalAssetPath
        else
            null
    } catch (e: Exception) {
        null
    }
}

suspend fun HttpResponse.toAttachmentAsset(): String? {
    return try {
        if (this.status == HttpStatusCode.OK)
            this.body<ApiResponse>().id
        else
            null
    } catch (e: Exception) {
        null
    }
}

/**
 * Converts Bitmap to file
 * @param context Context
 * @param outputPathFolder Folder name for storing the png file eg (images, media, custom)
 */
fun Bitmap?.toFile(context: Context, outputPathFolder: String): File {
    val dir = File(context.filesDir.toString() + File.separator + outputPathFolder)
    dir.mkdirs()
    val image = File(dir, "Temp.png")
    FileOutputStream(image).use { out ->
        val target = this ?: Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
        val maxDim = 512
        val scaled = if (target.width > maxDim || target.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(target.width, target.height)
            val newW = (target.width * scale).toInt().coerceAtLeast(1)
            val newH = (target.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(target, newW, newH, true)
        } else {
            target
        }
        scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return image
}

fun Context.getAppBitmap(packageName: String): Bitmap? {
    return try {
        val drawable = packageManager.getApplicationIcon(packageName)
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 192
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 192
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    } catch (e: Exception) {
        try {
            val drawable = com.blankj.utilcode.util.AppUtils.getAppIcon(packageName)
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else if (drawable != null) {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 192
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 192
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

@Suppress("DEPRECATION")
fun Context.getAppInfo(packageName: String): ApplicationInfo {
    return this.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
}

@Suppress("DEPRECATION")
fun ApplicationInfo.toBitmap(context: Context): Bitmap? {
    return context.getAppBitmap(this.packageName)
}
fun String.toRpcImage(): RpcImage? {
    return when {
        this.isBlank() -> null
        this.startsWith("mp:") -> RpcImage.DiscordImage(this)
        this.startsWith("attachments") || this.startsWith("external") -> RpcImage.DiscordImage("mp:$this")
        this.startsWith("http://") || this.startsWith("https://") -> RpcImage.ExternalImage(this)
        else -> RpcImage.DiscordImage(this)
    }
}

fun Context.getFileName(uri: Uri): String = "temp_file.${getFileExtension(this, uri)}"

private fun getFileExtension(context: Context, uri: Uri): String? =
    if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri))
    } else {
        uri.path?.let { MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(it)).toString()) }
    }

fun Context.uriToFile(uri: Uri): File {
    val file = File(cacheDir,getFileName(uri))
    val inputStream = contentResolver.openInputStream(uri)
    inputStream?.use { input ->
        file.outputStream().use { out ->
            input.copyTo(out)
        }
    }
    return file
}

fun Context.shareAsFile(content: String?, fileName: String) {
    val tempFile = File("$filesDir/$fileName")
    if (FileUtils.isFileExists(tempFile)) {
        FileUtils.delete(tempFile)
    }
    FileIOUtils.writeFileFromString(tempFile, content)
    shareFile(tempFile)
}

fun Context.shareFile(file: File?) {
    if (file == null) return
    val uri = FileProvider.getUriForFile(this, "com.my.kizzy.provider", file)
    val intent = ShareCompat.IntentBuilder(this).setType("text/plain")
        .setStream(uri).intent.setAction(Intent.ACTION_SEND).setDataAndType(uri, "text/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    this.startActivity(Intent.createChooser(intent, "Share File With"))
}