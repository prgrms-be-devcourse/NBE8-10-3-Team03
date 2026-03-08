package com.back.domain.post.post.service.port

import com.back.domain.post.post.dto.PostListResponse
import com.back.domain.post.post.entity.Post
import com.back.domain.post.post.entity.PostStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Slice

interface PostPort {
    fun save(post: Post): Post
    fun findByIdAndDeletedFalse(id: Int): Post?
    fun findPostsByStatus(status: PostStatus?, pageable: Pageable): Page<Post>
    fun search(kw: String, pageable: Pageable): Page<Post>
    fun findAllByDeletedFalse(pageable: Pageable): Page<Post>
    fun findBySellerId(sellerId: Int, pageable: Pageable): Page<Post>
    fun findBySellerIdAndStatus(sellerId: Int, status: PostStatus, pageable: Pageable): Page<Post>
    fun findSliceProjectionByCategoryIdAndStatus(categoryId: Int,  status: PostStatus, pageable: Pageable): Slice<Post>
    fun findSliceProjectionByCategoryId(categoryId: Int, pageable: Pageable): Slice<Post>
    fun findSliceProjectionByStatus(status: PostStatus,  pageable: Pageable): Slice<Post>
    fun findSliceProjectionAll(pageable: Pageable): Slice<Post>
    fun countByCategoryIdAndStatus(categoryId: Int, status: PostStatus): Long
    fun countByCategoryId(categoryId: Int): Long
    fun countByStatus(status: PostStatus): Long
    fun countAll(): Long

}