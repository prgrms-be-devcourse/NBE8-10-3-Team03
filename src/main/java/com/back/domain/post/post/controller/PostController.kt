package com.back.domain.post.post.controller

import com.back.domain.post.post.dto.*
import com.back.domain.post.post.service.PostService
import com.back.domain.post.post.service.port.PostUseCase
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@Tag(name = "중고거래", description = "중고거래 게시글 등록, 조회, 수정, 삭제 및 상태 관리 API")
@RestController
@RequestMapping("/api/v1/posts")
class PostController(
    private val postUseCase: PostUseCase,
    private val rq: Rq
) {

    @Operation(summary = "중고거래 글 작성", description = "새로운 중고거래 게시글을 작성합니다. **제목(2-50자)**, **내용(10-1000자)** 제한이 있으며, **정지된 회원**은 등록이 차단됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "게시글 등록 성공"),
        ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패 (제목/내용 길이 등)"),
        ApiResponse(responseCode = "401", description = "로그인 필요 (401-1)"),
        ApiResponse(responseCode = "403", description = "정지 회원 활동 제한 (403-3)"),
        ApiResponse(responseCode = "404", description = "카테고리 정보 없음 (404-2)")
    )
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun create(@Valid @ModelAttribute req: PostCreateRequest): RsData<PostIdResponse> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        val id = postUseCase.create(actor, req)
        return RsData("201-1", "게시글 등록이 완료되었습니다", PostIdResponse(id, "등록 완료"))
    }

    @Operation(summary = "게시글 수정", description = "기존 게시글의 제목, 내용, 가격 등을 수정합니다. 기존 이미지를 유지하거나 새 이미지를 추가할 수 있습니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수정 성공"),
        ApiResponse(responseCode = "403", description = "본인 글만 수정 가능 (403-1) / 정지 회원 차단 (403-3)")
    )
    @PatchMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun modify(
        @PathVariable id: Int,
        @Valid @ModelAttribute req: PostUpdateRequest
    ): RsData<PostIdResponse> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        postUseCase.modify(actor, id, req)
        return RsData("200-1", "수정 성공", PostIdResponse(id, "수정 완료"))
    }

    @Operation(summary = "게시글 상태 변경", description = "상품의 상태(SALE, RESERVED, SOLD)를 변경합니다.")
    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Int,
        @Valid @RequestBody req: PostStatusRequest
    ): RsData<PostIdResponse> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        val status = req.status ?: throw ServiceException("400", "상태값이 누락되었습니다.")

        postUseCase.updatePostStatus(actor, id, status)
        return RsData("200-1", "수정 성공", PostIdResponse(id, "수정 완료"))
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제 처리합니다. (논리적 삭제 적용)")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Int): RsData<PostIdResponse> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        postUseCase.delete(actor, id)
        return RsData("200-2", "삭제 성공", PostIdResponse(id, "삭제 완료"))
    }

    @Operation(summary = "상세 조회", description = "게시글의 상세 정보를 조회합니다. 판매자의 **신용 점수(Score)**가 함께 반환됩니다.")
    @GetMapping("/{id}")
    fun getDetail(@PathVariable id: Int): RsData<PostDetailResponse> {
        return RsData("200-3", "상세 조회 성공", postUseCase.getDetail(id))
    }

    @Operation(summary = "목록 조회", description = "상태별 필터링이 가능한 페이징 목록입니다. 판매자의 **뱃지(Badge)** 정보가 포함됩니다.")
    @GetMapping
    fun getList(
        @PageableDefault(size = 10, sort = ["createDate"], direction = Sort.Direction.DESC) pageable: Pageable,
        @Parameter(description = "필터링할 상태 (all, sale, reserved, sold)", example = "sale")
        @RequestParam(required = false) status: String?
    ): RsData<PostPageResponse> {
        val response = postUseCase.getList(pageable, status)
        return RsData("200-4", "목록 조회 성공", response)
    }
}