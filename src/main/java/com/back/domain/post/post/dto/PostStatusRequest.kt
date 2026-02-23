package com.back.domain.post.post.dto

import com.back.domain.post.post.entity.PostStatus

data class PostStatusRequest(
    var status: PostStatus? = null
)