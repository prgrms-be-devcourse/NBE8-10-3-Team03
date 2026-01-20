package com.back.domain.member.member.controller;

import com.back.domain.member.member.dto.MemberDto;
import com.back.domain.member.member.dto.MemberWithUsernameDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class ApiV1MemberController {
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
    public RsData<MemberDto> join(
            @Valid @RequestBody MemberJoinReqBody reqBody
    ) {
        Member member = memberService.join(
                reqBody.username(),
                reqBody.password(),
                reqBody.nickname()
        );

        return new RsData<>(
                "201-1",
                "%s님 환영합니다. 회원가입이 완료되었습니다.".formatted(member.getName()),
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

    record MemberLoginResBody(
            MemberDto item,
            String apiKey,
            String accessToken
    ) {
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    @Operation(summary = "로그인")
    public RsData<MemberLoginResBody> login(
            @Valid @RequestBody MemberLoginReqBody reqBody
    ) {
        Member member = memberService.findByUsername(reqBody.username())
                .orElseThrow(() -> new ServiceException("401-1", "존재하지 않는 아이디입니다."));

        memberService.checkPassword(
                member,
                reqBody.password()
        );

        String accessToken = memberService.genAccessToken(member);

        rq.setCookie("apiKey", member.getApiKey());
        rq.setCookie("accessToken", accessToken);

        return new RsData<>(
                "200-1",
                "%s님 환영합니다.".formatted(member.getName()),
                new MemberLoginResBody(
                        new MemberDto(member),
                        member.getApiKey(),
                        accessToken
                )
        );
    }

    @Operation(summary = "로그아웃")
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
    public MemberWithUsernameDto me() {
        Member actor = memberService
                .findById(rq.getActor().getId())
                .get();

        return new MemberWithUsernameDto(actor);
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
    @Transactional
    @PatchMapping("/{userId}/credit")
    public RsData<Void> decrease(@PathVariable int userId) {
        Member member = memberService.findById(userId).get();

        memberService.decreaseByNofiy(member);

        return new RsData<>(
                "200-1",
                "신고 완료 처리되었습니다."
        );
    }


}
