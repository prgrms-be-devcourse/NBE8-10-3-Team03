package com.back.domain.post.post.repository;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Integer> {

    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.deleted = false")
    Optional<Post> findByIdAndDeletedFalse(@Param("id") int id);

    @Query(value = "SELECT p FROM Post p " +
            "WHERE p.deleted = false " +
            "AND (p.title LIKE %:kw% OR p.content LIKE %:kw%)",
            countQuery = "SELECT count(p) FROM Post p WHERE p.deleted = false AND (p.title LIKE %:kw% OR p.content LIKE %:kw%)")
    Page<Post> search(@Param("kw") String kw, Pageable pageable);

    @Query(value = "SELECT p FROM Post p " +
            "WHERE p.deleted = false " +
            "AND (:status IS NULL OR p.status = :status)",
            countQuery = "SELECT count(p) FROM Post p WHERE p.deleted = false AND (:status IS NULL OR p.status = :status)")
    Page<Post> findPostsByStatus(@Param("status") PostStatus status, Pageable pageable);

    @Query(value = "SELECT p FROM Post p WHERE p.deleted = false",
            countQuery = "SELECT count(p) FROM Post p WHERE p.deleted = false")
    Page<Post> findAllByDeletedFalse(Pageable pageable);

    Page<Post> findBySellerIdAndStatus(Integer sellerId, PostStatus status, Pageable pageable);
    Page<Post> findBySellerId(Integer sellerId, Pageable pageable);
}