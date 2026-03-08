package com.back.domain.post.post.repository

import com.back.domain.auction.auction.entity.AuctionStatus
import com.back.domain.post.post.dto.PostListProjection
import com.back.domain.post.post.dto.PostListResponse
import com.back.domain.post.post.entity.Post
import com.back.domain.post.post.entity.PostStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
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

    // ==========

    @Query(
        """
    SELECT new com.back.domain.post.post.dto.PostListProjection(
        p.id,
        p.title,
        p.price,
        :categoryName,
        p.thumbnailUrl,
        p.createDate,
        p.status
    )
    FROM Post p
    JOIN p.category c
    WHERE p.category.id = :categoryId
      AND p.status = :status
    """
    )
    fun findSliceProjectionByCategoryIdAndStatus(
        @Param("categoryId") categoryId: Int,
        @Param("categoryName") categoryName: String,
        @Param("status") status: PostStatus,
        pageable: Pageable
    ): Slice<PostListProjection>

    @Query(
        """
    SELECT new com.back.domain.post.post.dto.PostListProjection(
        p.id,
        p.title,
        p.price,
        :categoryName,
        p.thumbnailUrl,
        p.createDate,
        p.status
    )
    FROM Post p
    JOIN p.category c
    WHERE p.category.id = :categoryId
    """
    )
    fun findSliceProjectionByCategoryId(
        @Param("categoryId") categoryId: Int,
        @Param("categoryName") categoryName: String,
        pageable: Pageable
    ): Slice<PostListProjection>

    @Query(
        """
    SELECT new com.back.domain.post.post.dto.PostListProjection(
        p.id,
        p.title,
        p.price,
        c.name,
        p.thumbnailUrl,
        p.createDate,
        p.status
    )
    FROM Post p
    JOIN p.category c
    WHERE p.status = :status
    """
    )
    fun findSliceProjectionByStatus(
        @Param("status") status: PostStatus,
        pageable: Pageable
    ): Slice<PostListProjection>

    @Query(
        """
    SELECT new com.back.domain.post.post.dto.PostListProjection(
        p.id,
        p.title,
        p.price,
        c.name,
        p.thumbnailUrl,
        p.createDate,
        p.status
    )
    FROM Post p
    JOIN p.category c
    """
    )
    fun findSliceProjectionAll(
        pageable: Pageable
    ): Slice<PostListProjection>



    fun countByCategoryIdAndStatus(categoryId: Int, status: PostStatus): Long
    fun countByCategoryId(categoryId: Int): Long
    fun countByStatus(status: PostStatus): Long

}
