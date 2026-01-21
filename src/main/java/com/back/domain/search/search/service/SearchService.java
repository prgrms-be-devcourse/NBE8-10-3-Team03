package com.back.domain.search.search.service;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
import com.back.domain.search.search.dto.UnifiedSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {
    private final PostRepository postRepository;

    public Page<UnifiedSearchResponse> searchUnified(String keyword, Pageable pageable) {

        Page<Post> posts = postRepository.search(keyword, pageable);

        return posts.map(post -> UnifiedSearchResponse.builder()
            .id(post.getId())
            .type("POST")
            .title(post.getTitle())
            .price(post.getPrice())
            .status(post.getStatus().name())
            .categoryName(post.getCategory().getName())
            .thumbnailUrl(post.getPostImages().isEmpty() ? null 
                : post.getPostImages().get(0).getImage().getUrl())
            .createDate(post.getCreateDate())
            .build()
        );
    }
}