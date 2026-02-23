package com.back.domain.post.post.dto

data class PostPageResponse(
    val content: List<PostListResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val currentStatusFilter: String
)