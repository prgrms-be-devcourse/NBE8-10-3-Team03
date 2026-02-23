package com.back.domain.post.post.entity

import java.io.Serializable

data class PostImageId(
    var post: Int? = null,
    var image: Int? = null
) : Serializable