package com.back.domain.member.member.controller;

import com.back.domain.member.member.dto.MemberDto;
import com.back.domain.member.member.dto.MemberWithUsernameDto;
import com.back.domain.member.review.dto.ReviewDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "회원 관리", description = "회원 가입, 로그인, 로그아웃, 수정, 탈퇴, 신고 관련 API")
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final Rq rq;

    record MemberJoinReqBody(
            @NotBlank
            @Size(min = 2, max = 30)
            String username,
            @NotBlank
            @Size(min = 2, max = 30)
            String password,
            @NotBlank
            @Size(min = 2, max = 30)
            String nickname
    ) {
    }

    @PostMapping
    @Transactional
    @Operation(summary = "회원가입")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
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
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "resultCode": "409-1",
                      "msg": "이미 존재하는 아이디입니다.",
                      "data": null
                    }
                    """)
                    )
            )
    })
    public RsData<MemberDto> join(
            @Valid @RequestBody MemberJoinReqBody reqBody
    ) {
        Member member = memberService.join(
                reqBody.username(),
                reqBody.password(),
                reqBody.nickname(),
                null
        );

        return new RsData<>(
                "201-1",
                "%s님 환영합니다. 회원가입이 완료되었습니다.".formatted(member.getNickname()),
                new MemberDto(member)
        );
    }

    record MemberLoginReqBody(
            @NotBlank
            @Size(min = 2, max = 30)
            String username,
            @NotBlank
            @Size(min = 2, max = 30)
            String password
    ) {
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    @Operation(summary = "로그인")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
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
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                          "resultCode": "403-3",
                          "msg": "계정이 영구 정지되었습니다. 관리자에게 문의해주세요.",
                          "data": null
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                          "resultCode": "403-4",
                          "msg": "탈퇴한 계정입니다.",
                          "data": null
                    }
                    """)
                    )
            )
    })
    public RsData<MemberDto> login(
            @Valid @RequestBody MemberLoginReqBody reqBody
    ) {
        Member member = memberService.findByUsername(reqBody.username())
                .orElseThrow(() -> new ServiceException("401-1", "존재하지 않는 아이디입니다."));

        memberService.login(member, reqBody.password());

        String accessToken = memberService.genAccessToken(member);


        rq.setHeader("Authorization", "Bearer " + member.getApiKey() + " " + accessToken);
        rq.setCookie("apiKey", member.getApiKey());
        rq.setCookie("accessToken", accessToken);

        return new RsData<>(
                "200-1",
                "%s님 환영합니다.".formatted(member.getNickname()),
                new MemberDto(member)
        );
    }

    @Operation(summary = "로그아웃")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                            "resultCode": "200-1",
                                                            "msg": "로그아웃 되었습니다.",
                                                            "data": null
                                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "401-1",
                                                        "msg": "로그인 후 이용해주세요.",
                                                        "data": null
                                                    }
                                    """)
                    )
            )
    })
    @DeleteMapping("/logout")
    public RsData<Void> logout() {
        rq.deleteCookie("apiKey");
        rq.deleteCookie("accessToken");

        return new RsData<>(
                "200-1",
                "로그아웃 되었습니다."
        );
    }


    @GetMapping("/me")
    @Transactional(readOnly = true)
    @Operation(summary = "내 정보 조회")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "id": 6,
                                                        "createDate": "2026-01-22T15:53:00.332242",
                                                        "modifyDate": "2026-01-22T15:53:00.334658",
                                                        "name": "유저4",
                                                        "username": "user4",
                                                        "score": 50.0
                                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "401-1",
                                                        "msg": "로그인 후 이용해주세요.",
                                                        "data": null
                                                    }
                                    """)
                    )
            )
    })
    public MemberWithUsernameDto me() {
        Member actor = rq.getActorFromDb();

        return new MemberWithUsernameDto(actor);
    }


    @PatchMapping("/me/withdraw")
    @Operation(summary = "회원 탈퇴")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "탈퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                          "resultCode": "200-1",
                                          "msg": "탙퇴가 정상적으로 처리되었습니다.",
                                          "data": null
                                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                          "resultCode": "404-1",
                          "msg": "존재하지 않는 회원입니다.",
                          "data": null
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "resultCode": "400-3",
                                        "msg": "탈퇴한 계정입니다.",
                                        "data": null
                                    }
                    """)
                    )
            )
    })
    public RsData<Void> withdraw() {
        Member actor = rq.getActorFromDb();
        rq.deleteCookie("apiKey");
        rq.deleteCookie("accessToken");
        memberService.withdraw(actor);

        return new RsData<>(
                "200-1",
                "탙퇴가 정상적으로 처리되었습니다."
        );
    }


    record MemberNameModifyReqBody(
            @NotBlank
            @Size(min = 2, max = 30)
            String nickname
    ) {
    }


    @PatchMapping("/me/nickname")
    @Transactional
    @Operation(summary = "닉네임 수정")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "200-1",
                                                        "msg": "수정이 완료되었습니다.",
                                                        "data": null
                                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "401-1",
                                                        "msg": "로그인 후 이용해주세요.",
                                                        "data": null
                                                    }
                                    """)
                    )
            )
    })
    public RsData<Void> modifyNickname(@Valid @RequestBody MemberNameModifyReqBody reqBody) {
        Member member = memberService
                .findById(rq.getActor().getId())
                .get();

        member.checkActorCanModify(member);

        memberService.modifyNickname(member, reqBody.nickname());

        return new RsData<>(
                "200-1",
                "수정이 완료되었습니다."
        );
    }

    record MemberPwModifyReqBody(
            @NotBlank
            @Size(min = 2, max = 30)
            String password,
            @NotBlank
            @Size(min = 2, max = 30)
            String newPassword,
            @NotBlank
            @Size(min = 2, max = 30)
            String checkPassword
    ) {
    }

    @PatchMapping("/me/password")
    @Transactional
    @Operation(summary = "비밀번호 수정")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "200-1",
                                                        "msg": "수정이 완료되었습니다.",
                                                        "data": null
                                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "401-1",
                                                        "msg": "로그인 후 이용해주세요.",
                                                        "data": null
                                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "401-1",
                                                        "msg": "현재 비밀번호와 일치하지 않습니다.",
                                                        "data": null
                                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "401-1",
                                                        "msg": "비밀번호가 일치하지 않습니다.",
                                                        "data": null
                                                    }
                                    """)
                    )
            )
    })
    public RsData<Void> modifyPassword(@Valid @RequestBody MemberPwModifyReqBody reqBody) {
        Member member = memberService
                .findById(rq.getActor().getId())
                .get();

        member.checkActorCanModify(member);

        memberService.modifyPassword(member, reqBody.password(), reqBody.newPassword(), reqBody.checkPassword());

        return new RsData<>(
                "200-1",
                "수정이 완료되었습니다."
        );
    }


    @Operation(summary = "신고 시 사용자 신용도 감소")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "200-1",
                                                        "msg": "신고가 완료되었습니다.",
                                                        "data": null
                                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "401-1",
                                                        "msg": "로그인 후 이용해주세요.",
                                                        "data": null
                                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "403-1",
                                                        "msg": "해당 회원에 대한 신고 횟수를 초과했습니다.",
                                                        "data": null
                                                    }
                                    """)
                    )
            )
    })
    @Transactional
    @PatchMapping("/{userId}/credit")
    public RsData<Void> decrease(@PathVariable int userId) {
        Member member = memberService.findById(userId).get();
        Member reporter = rq.getActor();

        memberService.decreaseByNofiy(member, reporter);

        return new RsData<>(
                "200-1",
                "신고 완료 처리되었습니다."
        );
    }

    record MemberReviewReqBody(
            @NotNull
            int star,
            @Nullable
            String comment
    ) {
    }

    @Operation(summary = "리뷰 남기기 (판매자 평가)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "201-1",
                                                        "msg": "후기 작성이 완료되었습니다.",
                                                        "data": null
                                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "401-1",
                                                        "msg": "로그인 후 이용해주세요.",
                                                        "data": null
                                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "403-3",
                                                        "msg": "정지된 회원은 해당 기능을 사용할 수 없습니다.",
                                                        "data": null
                                                    }
                                    """)
                    )
            )
    })
    @Transactional
    @PostMapping("/{userId}/review")
    public RsData<Void> review(@PathVariable int userId, @Valid @RequestBody MemberReviewReqBody request) {
        Member member = memberService.findById(userId).get();
        Member reviewer = rq.getActor();

        // 별점 1 ~ 5
        memberService.createReview(request.star(), request.comment(), member, reviewer.getId());

        return new RsData<>(
                "201-1",
                "후기 작성이 완료되었습니다."
        );
    }

    @Operation(summary = "리뷰(판매자 평가) 가져오기")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                                    {
                                                        "resultCode": "401-1",
                                                        "msg": "로그인 후 이용해주세요.",
                                                        "data": null
                                                    }
                                    """)
                    )
            )
    })
    @Transactional
    @GetMapping("/{userId}/review")
    public List<ReviewDto> getReviews(@PathVariable int userId) {
        Member member = memberService.findById(userId).get();

        return member
                .getReviews()
                .stream()
                .map(ReviewDto::new)
                .toList();
    }


}
