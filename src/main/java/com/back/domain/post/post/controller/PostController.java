package com.back.domain.post.post.controller;

import com.back.domain.post.post.dto.*;
import com.back.domain.post.post.service.PostService;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @PostMapping
    public RsData<PostDto> write(@Valid @RequestBody PostSaveRequest req) {
        return new RsData<>("201-1", "등록 성공", new PostDto(postService.write(null, req.title(), req.content(), req.price())));
    }

    @PatchMapping("/{id}")
    public RsData<Void> modify(@PathVariable int id, @Valid @RequestBody PostSaveRequest req) {
        postService.modify(id, req.title(), req.content(), req.price());
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