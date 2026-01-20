package com.back.domain.post.post.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.post.post.dto.PostDto;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;

    @Transactional
    public Post write(Member seller, String title, String content, int price) {
        return postRepository.save(Post.builder().seller(seller).title(title).content(content).price(price).build());
    }

    @Transactional
    public void modify(int id, String title, String content, int price) {
        Post post = postRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 글입니다."));
        post.update(title, content, price);
    }

    @Transactional
    public void delete(int id) {
        Post post = postRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 글입니다."));
        post.setDeleted(true);
    }

    public PostDto getDetail(int id) {
        return postRepository.findByIdAndDeletedFalse(id).map(PostDto::new)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 글입니다."));
    }

    public Page<PostDto> getList(int page) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by("createDate").descending());
        return postRepository.findAllByDeletedFalse(pageable).map(PostDto::new);
    }
}