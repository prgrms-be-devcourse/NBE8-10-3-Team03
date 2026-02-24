package com.back.global.rsData

import com.fasterxml.jackson.annotation.JsonIgnore

@JvmRecord
data class RsData<T>(
    val resultCode: String?,
    @JvmField @field:JsonIgnore
    val statusCode: Int,
    val msg: String?,
    val data: T?
) {
    @JvmOverloads
    constructor(resultCode: String, msg: String?, data: T? = null) : this(
        resultCode,
        resultCode.split("-", limit = 2)[0].toInt(),
        msg,
        data
    )
}
