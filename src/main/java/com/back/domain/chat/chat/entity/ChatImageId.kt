package com.back.domain.chat.chat.entity

import java.io.Serializable

data class ChatImageId(
    var chat: Int? = null,
    var image: Int? = null,
) : Serializable
