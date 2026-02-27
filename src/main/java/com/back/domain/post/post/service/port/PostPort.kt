package com.back.domain.post.post.service.port

import com.back.domain.post.post.entity.Post
import com.back.domain.post.post.entity.PostStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Page

interface PostPort {
    fun save(post: Post): Post
    fun findByIdAndDeletedFalse(id: Int): Post?
    fun findPostsByStatus(status: PostStatus?, pageable: Pageable): Page<Post>
    fun search(kw: String, pageable: Pageable): Page<Post>
    fun findAllByDeletedFalse(pageable: Pageable): Page<Post>
    fun findBySellerId(sellerId: Int, pageable: Pageable): Page<Post>
    fun findBySellerIdAndStatus(sellerId: Int, status: PostStatus, pageable: Pageable): Page<Post>
}