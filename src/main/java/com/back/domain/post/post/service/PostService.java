package com.back.domain.post.post.service;

import com.back.domain.auction.auction.service.FileStorageService;
import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.image.image.entity.Image;
import com.back.domain.image.image.repository.ImageRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.post.post.dto.*;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.entity.PostImage;
import com.back.domain.post.post.entity.PostStatus;
import com.back.domain.post.post.repository.PostRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

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
    public int create(Member actor, PostCreateRequest req) {

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ServiceException("404-2", "존재하지 않는 카테고리입니다."));

        Post post = Post.builder()
                .seller(actor)
                .title(req.getTitle())
                .content(req.getContent())
                .price(req.getPrice())
                .category(category)
                .status(PostStatus.SALE)
                .build();

        postRepository.save(post);

        if (req.getImages() != null && !req.getImages().isEmpty()) {
            for (MultipartFile file : req.getImages()) {
                if (file.isEmpty()) continue;

                try {
                    String imageUrl = fileStorageService.storeFile(file);

                    Image image = imageRepository.save(new Image(imageUrl));

                    PostImage postImage = new PostImage(post, image);
                    post.addPostImage(postImage);
                } catch (Exception e) {
                    throw new ServiceException("500-1", "이미지 저장에 실패했습니다: " + e.getMessage());
                }
            }
        }

        return post.getId();
    }

    @Transactional
    public void modify(Member actor, int id, PostUpdateRequest req) {
        Post post = postRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 글입니다."));

        if (post.getSeller().getId() != actor.getId()) {
            throw new ServiceException("403-1", "자신의 글만 수정할 수 있습니다.");
        }

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ServiceException("404-2", "카테고리 오류"));

        post.update(req.getTitle(), req.getContent(), req.getPrice(), category);

        updateImages(req, post);
    }

    private void updateImages(PostUpdateRequest req, Post post) {
        List<String> keepUrls = req.getKeepImageUrls();
        if (keepUrls == null || keepUrls.isEmpty()) {
            post.getPostImages().clear();
        } else {
            post.getPostImages().removeIf(pi -> !keepUrls.contains(pi.getImage().getUrl()));
        }

        if (req.getImages() != null) {
            for (MultipartFile file : req.getImages()) {
                if (file.isEmpty()) continue;
                String url = fileStorageService.storeFile(file);
                Image img = imageRepository.save(new Image(url));
                post.addPostImage(new PostImage(post, img));
            }
        }
    }

    @Transactional
    public void delete(Member actor, int id) {
        Post post = postRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 글입니다."));

        if (post.getSeller().getId() != actor.getId()) {
            throw new ServiceException("403-1", "자신의 글만 삭제할 수 있습니다.");
        }
        post.setDeleted(true);
    }

    public PostDetailResponse getDetail(int id) {
        Post post = postRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 글입니다."));
        return new PostDetailResponse(post);
    }

    public Page<PostListResponse> getList(int page) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by("createDate").descending());
        return postRepository.findAllByDeletedFalse(pageable).map(PostListResponse::new);
    }
}