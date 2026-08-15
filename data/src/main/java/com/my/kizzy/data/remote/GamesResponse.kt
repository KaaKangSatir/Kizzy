/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * GamesResponse.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.data.remote


import com.my.kizzy.data.rpc.Constants
import com.my.kizzy.domain.model.Game
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GamesResponse(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("icon_hash")
    val iconHash: String? = null,
    @SerialName("cover_image_hash")
    val coverImageHash: String? = null,
    @SerialName("icon")
    val iconId: String? = null,
    @SerialName("cover_image")
    val coverImageId: String? = null

)

fun GamesResponse.toGame() : Game {
    val actualIconHash = iconHash ?: iconId
    val actualCoverHash = coverImageHash ?: coverImageId
    
    // Priority: Cover -> Icon -> Empty
    // Send the raw hash instead of URL so Discord Gateway correctly binds it to the application ID
    val finalLargeImage = actualCoverHash ?: actualIconHash ?: ""
    val finalSmallImage = if (actualCoverHash != null && actualIconHash != null) actualIconHash else ""
    
    val imageUrl = if (actualCoverHash != null) "https://cdn.discordapp.com/app-icons/$id/$actualCoverHash.png"
                   else if (actualIconHash != null) "https://cdn.discordapp.com/app-icons/$id/$actualIconHash.png"
                   else Constants.XBOX_LINK
    
    return Game(
        platform = "PC",
        small_image = finalSmallImage,
        large_image = finalLargeImage,
        image_url = imageUrl,
        game_title = name,
        application_id = id
    )
}