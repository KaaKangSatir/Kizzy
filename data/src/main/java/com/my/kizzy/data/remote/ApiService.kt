/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * ApiService.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */
package com.my.kizzy.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import java.io.File
import javax.inject.Inject

import io.ktor.http.content.PartData
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully

class ApiService @Inject constructor(
    private val client: HttpClient,
    @Base private val baseUrl: String,
    @Github private val githubBaseUrl: String,
    @Discord private val discordBaseUrl: String,
) {
    suspend fun getImage(url: String) = runCatching {
        client.get {
            url("$baseUrl/image")
            parameter("url", url)
        }
    }

    suspend fun uploadImage(file: File) = runCatching {
        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable + kotlinx.coroutines.Dispatchers.IO) {
            val bytes = file.readBytes()
            val part = PartData.BinaryItem(
                provider = { buildPacket { writeFully(bytes) } },
                dispose = {},
                partHeaders = Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"temp\"; filename=\"Temp.png\"")
                    append(HttpHeaders.ContentType, "image/png")
                }
            )
            client.post {
                url("$baseUrl/upload")
                setBody(MultiPartFormDataContent(listOf(part)))
            }
        }
    }

    suspend fun getGames() = runCatching {
        client.get {
            url("$discordBaseUrl/applications/detectable")
        }
    }

    suspend fun getApplicationDetails(appId: String) = runCatching {
        client.get {
            url("$discordBaseUrl/oauth2/applications/$appId/rpc")
        }
    }

    suspend fun getUser(userid: String) = runCatching {
        client.get {
            url("$baseUrl/user/$userid")
        }
    }

    suspend fun getContributors() = runCatching {
        client.get {
            url("$baseUrl/contributors")
        }
    }

    suspend fun checkForUpdate() = runCatching {
        client.get {
            url("$githubBaseUrl/repos/dead8309/Kizzy/releases/latest")
        }
    }
}