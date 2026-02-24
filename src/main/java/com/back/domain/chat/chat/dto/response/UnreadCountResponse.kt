package com.back.domain.chat.chat.dto.response

@JvmRecord
data class UnreadCountResponse(
    @JvmField
    val roomId: String?,
    @JvmField
    val count: Long?
) 