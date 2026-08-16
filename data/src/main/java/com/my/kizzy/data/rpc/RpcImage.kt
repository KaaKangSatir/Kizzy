/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * RpcImage.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.data.rpc

import android.content.Context
import android.graphics.Bitmap
import com.my.kizzy.domain.repository.KizzyRepository
import com.my.kizzy.preference.Prefs
import com.my.kizzy.data.utils.getAppBitmap
import com.my.kizzy.data.utils.getAppInfo
import com.my.kizzy.data.utils.toBitmap
import com.my.kizzy.data.utils.toFile

sealed class RpcImage {
    abstract suspend fun resolveImage(repository: KizzyRepository): String?

    class DiscordImage(val image: String) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String {
            return if (image.startsWith("attachments/") || image.startsWith("external/")) {
                "mp:$image"
            } else {
                image
            }
        }
    }

    class ExternalImage(val image: String) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String? {
            return repository.getImage(image)
        }
    }

    class ApplicationIcon(val packageName: String, private val context: Context) : RpcImage() {
        companion object {
            private val appIconCache = java.util.concurrent.ConcurrentHashMap<String, String>()
        }

        override suspend fun resolveImage(repository: KizzyRepository): String? {
            val cached = appIconCache[packageName]
            return if (!cached.isNullOrBlank())
                cached
            else
                retrieveImageFromApi(packageName, context, repository)
        }

        private suspend fun retrieveImageFromApi(
            packageName: String,
            context: Context,
            repository: KizzyRepository,
        ): String? {
            return runCatching {
                val bitmap = context.getAppBitmap(packageName)
                val response = repository.uploadImage(bitmap.toFile(context, "image"))
                response?.let {
                    if (it.isNotBlank()) {
                        appIconCache[packageName] = it
                    }
                }
                response
            }.getOrNull()
        }
    }

    class BitmapImage(
        private val context: Context,
        val bitmap: Bitmap?,
        private val packageName: String,
        val title: String,
    ) : RpcImage() {
        companion object {
            private val artworkCache = java.util.concurrent.ConcurrentHashMap<String, String>()
        }

        override suspend fun resolveImage(repository: KizzyRepository): String? {
            val schema = "${this.packageName}:${this.title}"
            val cached = artworkCache[schema]
            return if (!cached.isNullOrBlank())
                cached
            else {
                val result = repository.uploadImage(bitmap.toFile(this.context, "art"))
                result?.let {
                    if (it.isNotBlank()) {
                        artworkCache[schema] = it
                    }
                }
                result
            }
        }
    }
}
