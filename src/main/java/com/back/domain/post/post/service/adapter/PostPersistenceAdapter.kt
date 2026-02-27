package com.back.domain.post.post.service.adapter


import com.back.domain.post.post.entity.Post
import com.back.domain.post.post.entity.PostStatus
import com.back.domain.post.post.repository.PostRepository
import com.back.domain.post.post.service.port.PostPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class PostPersistenceAdapter(
    private val postRepository: PostRepository
) : PostPort {

    override fun save(post: Post): Post = postRepository.save(post)

    override fun findByIdAndDeletedFalse(id: Int): Post? =
        postRepository.findByIdAndDeletedFalse(id)

    override fun findAllByDeletedFalse(pageable: Pageable): Page<Post> =
        postRepository.findAllByDeletedFalse(pageable)

    override fun search(kw: String, pageable: Pageable): Page<Post> =
        postRepository.search(kw, pageable)

    override fun findPostsByStatus(status: PostStatus?, pageable: Pageable): Page<Post> =
        postRepository.findPostsByStatus(status, pageable)

    override fun findBySellerId(sellerId: Int, pageable: Pageable): Page<Post> =
        postRepository.findBySellerId(sellerId, pageable)

    override fun findBySellerIdAndStatus(sellerId: Int, status: PostStatus, pageable: Pageable): Page<Post> =
        postRepository.findBySellerIdAndStatus(sellerId, status, pageable)
}