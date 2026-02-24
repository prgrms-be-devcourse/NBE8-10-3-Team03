package com.back.domain.member.member.controller

import com.back.domain.auction.auction.dto.response.AuctionSliceResponse
import com.back.domain.auction.auction.service.AuctionService
import com.back.domain.member.member.dto.MemberDto
import com.back.domain.member.member.dto.MemberWithUsernameDto
import com.back.domain.member.member.service.MemberService
import com.back.domain.member.review.dto.ReviewDto
import com.back.domain.post.post.dto.PostPageResponse
import com.back.domain.post.post.service.PostService
import com.back.global.audit.enums.AuditType
import com.back.global.audit.service.SecurityAuditService
import com.back.global.exception.ServiceException
import com.back.global.rateLimit.Bucket4jRateLimiter
import com.back.global.rateLimit.RateLimitPolicy
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@Tag(name = "회원 관리", description = "회원 가입, 로그인, 로그아웃, 수정, 탈퇴, 신고 관련 API")
@RequestMapping("/api/v1/members")
open class MemberController(
    private val memberService: MemberService,
    private val auctionService: AuctionService,
    private val postService: PostService,
    private val auditService: SecurityAuditService,
    private val servletRequest: HttpServletRequest,
    private val rateLimiter: Bucket4jRateLimiter,
    private val rq: Rq,
) {

    // ========================
    // Request/Response Bodies
    // ========================

    data class MemberJoinReqBody(
        @field:NotBlank(message = "아이디는 공백을 포함할 수 없습니다.")
        @field:Pattern(regexp = "^[^\\s]+$", message = "아이디는 공백을 포함할 수 없습니다.")
        @field:Size(min = 5, max = 20, message = "아이디는 5-20자여야 합니다.")
        val username: String,

        @field:NotBlank(message = "비밀번호는 필수입니다.")
        @field:Size(min = 8, max = 20, message = "비밀번호는 8-20자여야 합니다.")
        val password: String,

        @field:Size(min = 2, max = 30, message = "두 글자 이상 입력하세요.")
        val nickname: String,
    )

    data class MemberLoginReqBody(
        @field:NotBlank
        @field:Size(min = 2, max = 30)
        val username: String,

        @field:NotBlank
        @field:Size(min = 2, max = 30)
        val password: String,
    )

    data class MemberLoginResBody(
        val item: MemberDto?,
        val apiKey: String?,
        val accessToken: String?,
    )

    data class MemberNameModifyReqBody(
        @field:NotBlank
        @field:Size(min = 2, max = 30)
        val nickname: String,
    )

    data class MemberPwModifyReqBody(
        @field:NotBlank
        @field:Size(min = 2, max = 30)
        val password: String,

        @field:NotBlank
        @field:Size(min = 2, max = 30)
        val newPassword: String,

        @field:NotBlank
        @field:Size(min = 2, max = 30)
        val checkPassword: String,
    )

    data class MemberReviewReqBody(
        @field:NotNull
        val star: Int,
        val comment: String?,
    )

    // ========================
    // Endpoints
    // ========================

    @PostMapping
    @Transactional
    @Operation(summary = "회원가입")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "회원가입 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """
                                {
                                    "resultCode": "200-1",
                                    "msg": "유저1님 환영합니다.",
                                    "data": {
                                        "item": {
                                            "id": 3,
                                            "createDate": "2026-01-22T15:53:00.055929",
                                            "modifyDate": "2026-01-22T15:53:00.057929",
                                            "name": "유저1"
                                        }
                                    }
                                }
                            """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "409",
                description = "잘못된 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """
                                {
                                  "resultCode": "409-1",
                                  "msg": "이미 존재하는 아이디입니다.",
                                  "data": null
                                }
                            """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """
                                {
                                  "resultCode": "400-1",
                                  "msg": "아이디는 5-20자여야 합니다. 아이디는 공백을 포함할 수 없습니다.",
                                  "data": null
                                }
                            """
                            )
                        ]
                    )
                ]
            )
        ]
    )
    fun join(@RequestBody @Valid reqBody: MemberJoinReqBody): RsData<MemberDto?> {
        val member = memberService.join(
            reqBody.username,
            reqBody.password,
            reqBody.nickname,
            null,
        )

        return RsData(
            "201-1",
            "${member.nickname}님 환영합니다. 회원가입이 완료되었습니다.",
            MemberDto(member),
        )
    }

    @PostMapping("/login")
    @Transactional
    @Operation(summary = "로그인")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그인 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """
                                {
                                    "resultCode": "200-1",
                                    "msg": "유저1님 환영합니다.",
                                    "data": {
                                        "id": 3,
                                        "createDate": "2026-01-22T15:53:00.055929",
                                        "modifyDate": "2026-01-22T15:53:00.057929",
                                        "name": "유저1"
                                    }
                                }
                            """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "403",
                description = "잘못된 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """
                                {
                                    "resultCode": "403-3",
                                    "msg": "계정이 영구 정지되었습니다. 관리자에게 문의해주세요.",
                                    "data": null
                                }
                            """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                description = "잘못된 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """
                                {
                                    "resultCode": "401-1",
                                    "msg": "비밀번호 입력 횟수를 초과하였습니다. 10분 뒤에 다시 시도해주세요.",
                                    "data": null
                                }
                            """
                            )
                        ]
                    )
                ]
            )
        ]
    )
    fun login(
        @RequestBody @Valid reqBody: MemberLoginReqBody,
        request: HttpServletRequest,
    ): RsData<MemberLoginResBody?> {
        val member = memberService.findByUsername(reqBody.username)?.orElse(null)
            ?: throw ServiceException("401-1", "존재하지 않는 아이디입니다.")

        val key = "login:${request.remoteAddr}:${reqBody.username}"

        if (!rateLimiter.tryConsume(key, RateLimitPolicy.LOGIN)) {
            throw ServiceException("429-1", "요청이 너무 많습니다.")
        }

        return try {
            memberService.login(member, reqBody.password)

            val accessToken = memberService.genAccessToken(member)

            rq.setHeader("Authorization", "Bearer ${member.apiKey} $accessToken")
            rq.setCookie("apiKey", member.apiKey)
            rq.setCookie("accessToken", accessToken)

            RsData(
                "200-1",
                "${member.nickname}님 환영합니다.",
                MemberLoginResBody(MemberDto(member), member.apiKey, accessToken),
            )
        } catch (e: ServiceException) {
            auditService.log(
                null,
                AuditType.LOGIN_FAIL,
                servletRequest.remoteAddr,
                servletRequest.getHeader("User-Agent"),
            )
            throw e
        }
    }

    @DeleteMapping("/logout")
    @Operation(summary = "로그아웃")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그아웃 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """
                                {
                                    "resultCode": "200-1",
                                    "msg": "로그아웃 되었습니다.",
                                    "data": null
                                }
                            """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """
                                {
                                    "resultCode": "401-1",
                                    "msg": "로그인 후 이용해주세요.",
                                    "data": null
                                }
                            """
                            )
                        ]
                    )
                ]
            )
        ]
    )
    fun logout(): RsData<Void?> {
        rq.deleteCookie("apiKey")
        rq.deleteCookie("accessToken")

        return RsData("200-1", "로그아웃 되었습니다.")
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    @Operation(summary = "내 정보 조회")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패")
    )
    fun me(): MemberWithUsernameDto = MemberWithUsernameDto(rq.actorFromDb)

    @GetMapping("/me/auctions")
    @Transactional(readOnly = true)
    @Operation(summary = "내 경매 조회")
    fun getAuctionsById(
        @Parameter(description = "페이지 번호", example = "0") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "정렬", example = "createdAt,desc") @RequestParam(required = false) sort: String?,
        @Parameter(description = "상태", example = "OPEN") @RequestParam(required = false) status: String?,
    ): RsData<AuctionSliceResponse?>? {
        val member = rq.actor
        return auctionService.getAuctionsByUserId(member.id, page, size, sort, status)
    }

    @GetMapping("/me/posts")
    @Transactional(readOnly = true)
    @Operation(summary = "내 거래 조회")
    fun getPostsById(
        @PageableDefault(size = 10, sort = ["createDate"], direction = Sort.Direction.DESC) pageable: Pageable,
        @RequestParam(required = false) status: String?,
    ): RsData<PostPageResponse?> {
        val member = rq.actor
        val response = postService.getListByUserId(pageable, member.id, status)
        return RsData("200-4", "목록 조회 성공", response)
    }

    @PatchMapping("/me/withdraw")
    @Operation(summary = "회원 탈퇴")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "탈퇴 성공"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 회원"),
        ApiResponse(responseCode = "400", description = "이미 탈퇴한 계정"),
    )
    fun withdraw(): RsData<Void?> {
        val actor = rq.actorFromDb
        rq.deleteCookie("apiKey")
        rq.deleteCookie("accessToken")
        memberService.withdraw(actor)

        return RsData("200-1", "탈퇴가 정상적으로 처리되었습니다.")
    }

    @PatchMapping("/me/nickname")
    @Transactional
    @Operation(summary = "닉네임 수정")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수정 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
    )
    fun modifyNickname(@RequestBody @Valid reqBody: MemberNameModifyReqBody): RsData<Void?> {
        val member = memberService.findById(rq.actor.id).get()
        member.checkActorCanModify(member)
        memberService.modifyNickname(member, reqBody.nickname)

        return RsData("200-1", "수정이 완료되었습니다.")
    }

    @PatchMapping("/me/password")
    @Transactional
    @Operation(summary = "비밀번호 수정")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수정 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
    )
    fun modifyPassword(@RequestBody @Valid reqBody: MemberPwModifyReqBody): RsData<Void?> {
        val member = memberService.findById(rq.actor.id).get()
        member.checkActorCanModify(member)
        memberService.modifyPassword(member, reqBody.password, reqBody.newPassword, reqBody.checkPassword)

        return RsData("200-1", "수정이 완료되었습니다.")
    }

    @PatchMapping("/{userId}/credit")
    @Transactional
    @Operation(summary = "신고 시 사용자 신용도 감소")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "신고 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "403", description = "신고 횟수 초과"),
        ApiResponse(responseCode = "400", description = "일일 신고 횟수 초과 또는 중복 신고"),
    )
    fun decrease(@PathVariable userId: Int): RsData<Void?> {
        val member = memberService.findById(userId).get()
        val reporter = rq.actor
        memberService.decreaseByNofiy(member, reporter)

        return RsData("200-1", "신고 완료 처리되었습니다.")
    }

    @PostMapping("/{userId}/review")
    @Transactional
    @Operation(summary = "리뷰 남기기 (판매자 평가)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "리뷰 작성 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "403", description = "정지된 회원"),
    )
    fun review(
        @PathVariable userId: Int,
        @RequestBody @Valid request: MemberReviewReqBody,
    ): RsData<Void?> {
        val member = memberService.findById(userId).get()
        val reviewer = rq.actor
        memberService.createReview(request.star, request.comment, member, reviewer.id)

        return RsData("201-1", "후기 작성이 완료되었습니다.")
    }

    @GetMapping("/{userId}/review")
    @Transactional
    @Operation(summary = "리뷰(판매자 평가) 가져오기")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
    )
    fun getReviews(@PathVariable userId: Int): List<ReviewDto>? {
        val member = memberService.findById(userId).get()
        return member.reviews.map { review -> ReviewDto(review) }
    }

    @PatchMapping("/me/profile", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "프로필 사진 수정 (이미지 파일 업로드)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수정 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 요청"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
    )
    fun modifyProfile(
        @Parameter(
            description = "업로드할 프로필 이미지 파일 (multipart/form-data)",
            required = true,
            content = [Content(schema = Schema(type = "string", format = "binary"))],
        )
        @RequestPart("profileImg") profileImg: MultipartFile,
    ): RsData<Void?> {
        val actor = rq.actor
        memberService.modifyProfile(actor.id, profileImg)

        return RsData("200-1", "프로필 사진이 수정되었습니다.")
    }
}