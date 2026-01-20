package com.back.domain.post.post.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.post.post.dto.PostDto;
import com.back.domain.post.post.dto.PostSaveRequest;
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

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final Rq rq;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<PostDto> write(@Valid @ModelAttribute PostSaveRequest request) {
        Member actor = rq.getActor();

        if (actor == null) {
            throw new ServiceException("401-1", "로그인 후 이용해주세요.");
        }

        Post post = postService.write(actor, request);

        return new RsData<>("201-1", "게시글이 등록되었습니다.", new PostDto(post));
    }


    @PatchMapping("/{id}")
    public RsData<Void> modify(@PathVariable int id, @Valid @RequestBody PostSaveRequest req) {
        postService.modify(id, req);
        return new RsData<>("200-1", "수정 성공");
    }

    @DeleteMapping("/{id}")
    public RsData<Void> delete(@PathVariable int id) {
        postService.delete(id);
        return new RsData<>("200-2", "삭제 성공");
    }

    @GetMapping("/{id}")
    public RsData<PostDto> getDetail(@PathVariable int id) {
        return new RsData<>("200-3", "상세 조회 성공", postService.getDetail(id));
    }

    @GetMapping
    public RsData<Page<PostDto>> getList(@RequestParam(defaultValue = "0") int page) {
        return new RsData<>("200-4", "목록 조회 성공", postService.getList(page));
    }
}