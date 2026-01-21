package com.back.domain.post.post.repository;

import com.back.domain.post.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Integer> {

    Optional<Post> findByIdAndDeletedFalse(int id);

    @Query("SELECT p FROM Post p " +
            "WHERE p.deleted = false " +
            "AND (p.title LIKE %:kw% OR p.content LIKE %:kw%)")
    Page<Post> search(@Param("kw") String kw, Pageable pageable);

    Page<Post> findAllByDeletedFalse(Pageable pageable);
}