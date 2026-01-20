package com.back.domain.post.post.service;

import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.image.image.entity.Image;
import com.back.domain.image.image.repository.ImageRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.post.post.dto.PostDto;
import com.back.domain.post.post.dto.PostSaveRequest;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.entity.PostImage;
import com.back.domain.post.post.repository.PostRepository;
import com.back.domain.auction.auction.service.FileStorageService;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    private final MemberRepository memberRepository;

    @Transactional
    public Post write(Member actor, PostSaveRequest req) {

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ServiceException("404-2", "존재하지 않는 카테고리입니다."));

        Member seller = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException("404-1", "사용자를 찾을 수 없습니다."));

        Post post = postRepository.save(
                Post.builder()
                        .seller(seller)
                        .title(req.getTitle())
                        .content(req.getContent())
                        .price(req.getPrice())
                        .category(category)
                        .build()
        );


        if (req.getImages() != null) {
            for (MultipartFile file : req.getImages()) {
                if (file.isEmpty()) continue;

                String imageUrl = fileStorageService.storeFile(file);
                Image savedImage = imageRepository.save(new Image(imageUrl));
                post.addPostImage(new PostImage(post, savedImage));
            }
        }

        return post;
    }

    @Transactional
    public void modify(int id, PostSaveRequest req) {
        Post post = postRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 글입니다."));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ServiceException("404-2", "존재하지 않는 카테고리입니다."));


        post.update(req.getTitle(), req.getContent(), req.getPrice(), category);
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