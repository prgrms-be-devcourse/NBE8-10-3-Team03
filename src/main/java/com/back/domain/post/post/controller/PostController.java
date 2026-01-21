package com.back.domain.post.post.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.post.post.dto.*;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final Rq rq;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<PostIdResponse> create(
            @Valid @ModelAttribute PostCreateRequest req
    ) {
        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException("401-1", "로그인이 필요합니다.");
        }

        int id = postService.create(actor, req);

        return new RsData<>("201-1", "게시글 등록이 완료되었습니다", new PostIdResponse(id, "등록 완료"));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<PostIdResponse> modify(
            @PathVariable int id,
            @Valid @ModelAttribute PostUpdateRequest req
    ) {
        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException("401-1", "로그인이 필요합니다.");
        }

        postService.modify(actor, id, req);

        return new RsData<>("200-1", "수정 성공", new PostIdResponse(id, "수정 완료"));
    }

    @PatchMapping("/{id}/status")
    public RsData<PostIdResponse> updateStatus(@PathVariable int id, @Valid @RequestBody PostStatusRequest req) {
        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException("401-1", "로그인이 필요합니다.");
        }

        postService.updatePostStatus(actor, id, req.getStatus());
        return new RsData<>("200-1", "수정 성공", new PostIdResponse(id, "수정 완료"));
    }

    @DeleteMapping("/{id}")
    public RsData<PostIdResponse> delete(
            @PathVariable int id
    ) {
        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException("401-1", "로그인이 필요합니다.");
        }

        postService.delete(actor, id);

        return new RsData<>("200-2", "삭제 성공", new PostIdResponse(id, "삭제 완료"));
    }

    @GetMapping("/{id}")
    public RsData<PostDetailResponse> getDetail(@PathVariable int id) {
        return new RsData<>("200-3", "상세 조회 성공", postService.getDetail(id));
    }

    @GetMapping
    public RsData<Page<PostListResponse>> getList(
            @PageableDefault(size = 10, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return new RsData<>("200-4", "목록 조회 성공", postService.getList(pageable));
    }
}