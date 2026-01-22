package com.back.domain.post.post.service;

import com.back.domain.auction.auction.service.FileStorageService;
import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.image.image.entity.Image;
import com.back.domain.image.image.repository.ImageRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.MemberStatus;
import com.back.domain.member.member.service.MemberService;
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
    private final MemberService memberService;

    @Transactional
    public int create(Member actor, PostCreateRequest req) {
        // 정지된 회원의 글쓰기 방지
        if(memberService.findById(actor.getId()).get().getStatus() == MemberStatus.SUSPENDED) {
            throw new ServiceException("403-3", "정지된 회원은 해당 기능을 사용할 수 없습니다.");
        }

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

        if(memberService.findById(actor.getId()).get().getStatus() == MemberStatus.SUSPENDED) {
            throw new ServiceException("403-3", "정지된 회원은 해당 기능을 사용할 수 없습니다.");
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

    @Transactional
    public PostDetailResponse getDetail(int id) {
        Post post = postRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 글입니다."));

        post.increaseViewCount();
        return new PostDetailResponse(post);
    }

    public Page<PostListResponse> getList(Pageable pageable) {
        return postRepository.findAllByDeletedFalse(pageable)
                .map(PostListResponse::new);
    }

    @Transactional
    public void updatePostStatus(Member actor, int id, PostStatus status) {
        Post post = postRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 글입니다."));

        if (post.getSeller().getId() != actor.getId()) {
            throw new ServiceException("403-1", "자신의 글만 상태를 수정할 수 있습니다.");
        }

        post.updateStatus(status);
    }

    public PostPageResponse getList(Pageable pageable, String statusStr) {
        PostStatus status = (statusStr != null && !"all".equalsIgnoreCase(statusStr))
                ? PostStatus.valueOf(statusStr.toUpperCase()) : null;

        Page<Post> postPage = postRepository.findPostsByStatus(status, pageable);

        List<PostListResponse> dtoList = postPage.getContent().stream()
                .map(PostListResponse::new)
                .toList();

        return new PostPageResponse(
                dtoList, postPage.getNumber(), postPage.getSize(),
                postPage.getTotalElements(), postPage.getTotalPages(),
                statusStr != null ? statusStr : "all"
        );
    }

    public PostPageResponse getListByUserId(Pageable pageable, int userId, String statusStr) {
        PostStatus status = (statusStr != null && !"all".equalsIgnoreCase(statusStr))
                ? PostStatus.valueOf(statusStr.toUpperCase()) : null;

        Page<Post> postPage;

        if (status == null) {
            postPage = postRepository.findBySellerId(userId, pageable);
        }
        else postPage = postRepository.findBySellerIdAndStatus(userId, status, pageable);

        List<PostListResponse> dtoList = postPage.getContent().stream()
                .map(PostListResponse::new)
                .toList();

        return new PostPageResponse(
                dtoList, postPage.getNumber(), postPage.getSize(),
                postPage.getTotalElements(), postPage.getTotalPages(),
                statusStr != null ? statusStr : "all"
        );
    }
}