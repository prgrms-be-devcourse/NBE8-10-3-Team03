package com.back.domain.post.post.repository

import com.back.domain.post.post.entity.Post
import com.back.domain.post.post.entity.PostStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<Post, Int> {

    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.deleted = false")
    fun findByIdAndDeletedFalse(@Param("id") id: Int): Post?

    @Query(
        value = """
            SELECT p FROM Post p 
            WHERE p.deleted = false 
            AND (p.title LIKE %:kw% OR p.content LIKE %:kw%)
        """,
        countQuery = "SELECT count(p) FROM Post p WHERE p.deleted = false AND (p.title LIKE %:kw% OR p.content LIKE %:kw%)"
    )
    fun search(@Param("kw") kw: String, pageable: Pageable): Page<Post>

    @Query(
        value = """
            SELECT p FROM Post p 
            WHERE p.deleted = false 
            AND (:status IS NULL OR p.status = :status)
        """,
        countQuery = "SELECT count(p) FROM Post p WHERE p.deleted = false AND (:status IS NULL OR p.status = :status)"
    )
    fun findPostsByStatus(@Param("status") status: PostStatus?, pageable: Pageable): Page<Post>

    @Query(
        value = "SELECT p FROM Post p WHERE p.deleted = false",
        countQuery = "SELECT count(p) FROM Post p WHERE p.deleted = false"
    )
    fun findAllByDeletedFalse(pageable: Pageable): Page<Post>

    fun findBySellerIdAndStatus(sellerId: Int, status: PostStatus, pageable: Pageable): Page<Post>

    fun findBySellerId(sellerId: Int, pageable: Pageable): Page<Post>

    fun countByTitleStartingWith(prefix: String): Long

    fun deleteByTitleStartingWith(prefix: String): Long
}
