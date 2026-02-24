package com.back.domain.member.member.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.MemberStatus;
import com.back.domain.member.member.service.MemberService;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MemberControllerTest {
    @Autowired
    private MemberService memberService;
    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("회원가입")
    void t1() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "username": "usernew",
                                            "password": "alskdfjelkl312@",
                                            "nickname": "무명2"
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        Member member = memberService.findByUsername("usernew").orElse(null);

        resultActions
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("join"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("%s님 환영합니다. 회원가입이 완료되었습니다.".formatted(member.getNickname())))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(member.getId()))
                .andExpect(jsonPath("$.data.createDate").value(Matchers.startsWith(member.getCreateDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.data.modifyDate").value(Matchers.startsWith(member.getModifyDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.data.name").value(member.getNickname()));
    }

    @Test
    @DisplayName("로그인")
    void t2() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "username": "user1",
                                            "password": "1234"
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        Member member = memberService.findByUsername("user1").get();

        resultActions
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%s님 환영합니다.".formatted(member.getNickname())))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.item.id").value(member.getId()))
                .andExpect(jsonPath("$.data.item.createDate").value(Matchers.startsWith(member.getCreateDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.data.item.modifyDate").value(Matchers.startsWith(member.getModifyDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.data.item.name").value(member.getNickname()));

        resultActions.andExpect(
                result -> {
                    Cookie apiKeyCookie = result.getResponse().getCookie("apiKey");
                    assertThat(apiKeyCookie.getValue()).isEqualTo(member.getApiKey());
                    assertThat(apiKeyCookie.getPath()).isEqualTo("/");
                    assertThat(apiKeyCookie.isHttpOnly()).isTrue();

                    Cookie accessTokenCookie = result.getResponse().getCookie("accessToken");
                    assertThat(accessTokenCookie.getValue()).isNotBlank();
                    assertThat(accessTokenCookie.getPath()).isEqualTo("/");
                    assertThat(apiKeyCookie.isHttpOnly()).isTrue();
                }
        );
    }


    @Test
    @DisplayName("내 정보")
    @WithUserDetails("user1")
    void t3() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/members/me")
                )
                .andDo(print());

        Member member = memberService.findByUsername("user1").get();

        resultActions
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(member.getId()))
                .andExpect(jsonPath("$.createDate").value(Matchers.startsWith(member.getCreateDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.modifyDate").value(Matchers.startsWith(member.getModifyDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.name").value(member.getNickname()))
                .andExpect(jsonPath("$.username").value(member.getUsername()))
                .andExpect(jsonPath("$.score").value(member.getReputation().getScore()));
    }

    @Test
    @DisplayName("내 정보, with apiKey Cookie")
    void t4() throws Exception {
        Member actor = memberService.findByUsername("user1").get();
        String actorApiKey = actor.getApiKey();

        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/members/me")
                                .cookie(new Cookie("apiKey", actorApiKey))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("로그아웃")
    void t6() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/members/logout")
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("로그아웃 되었습니다."))
                .andExpect(result -> {
                    Cookie apiKeyCookie = result.getResponse().getCookie("apiKey");
                    assertThat(apiKeyCookie.getValue()).isEmpty();
                    assertThat(apiKeyCookie.getMaxAge()).isEqualTo(0);
                    assertThat(apiKeyCookie.getPath()).isEqualTo("/");
                    assertThat(apiKeyCookie.isHttpOnly()).isTrue();
                });
    }

    @Test
    @DisplayName("엑세스 토큰이 만료되었거나 유효하지 않다면 apiKey를 통해서 재발급")
    void t7() throws Exception {
        Member actor = memberService.findByUsername("user1").get();
        String actorApiKey = actor.getApiKey();

        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/members/me")
                                .header("Authorization", "Bearer " + actorApiKey + " wrong-access-token")
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk());

        resultActions.andExpect(
                result -> {
                    Cookie accessTokenCookie = result.getResponse().getCookie("accessToken");
                    assertThat(accessTokenCookie.getValue()).isNotBlank();
                    assertThat(accessTokenCookie.getPath()).isEqualTo("/");
                    assertThat(accessTokenCookie.isHttpOnly()).isTrue();

                    String headerAuthorization = result.getResponse().getHeader("Authorization");
                    assertThat(headerAuthorization).isNotBlank();

                    assertThat(headerAuthorization).isEqualTo(accessTokenCookie.getValue());
                }
        );
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer 형식이 아닐 때 오류")
    void t8() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/members/me")
                                .header("Authorization", "key")
                )
                .andDo(print());

        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-2"))
                .andExpect(jsonPath("$.msg").value("Authorization 헤더가 Bearer 형식이 아닙니다."));
    }

    @Test
    @DisplayName("회원 닉네임 수정")
    @WithUserDetails("user1")
    void t9() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        patch("/api/v1/members/me/nickname")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "nickname": "new 무명"
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("modifyNickname"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("수정이 완료되었습니다."));
    }

    @Test
    @DisplayName("비밀번호 수정")
    @WithUserDetails("user1")
    void t10() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        patch("/api/v1/members/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "password": "1234",
                                            "newPassword" : "new1234",
                                            "checkPassword" : "new1234"
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("modifyPassword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("수정이 완료되었습니다."));
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("신고 처리")
    void t11() throws Exception {
        Member user1 = memberService.findByUsername("user1").get();
        int userId = user1.getId();

        ResultActions resultActions = mvc
                .perform(
                        patch("/api/v1/members/%d/credit".formatted(userId))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("decrease"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("신고 완료 처리되었습니다."));

        assertThat(user1.getTargetReports()).hasSize(1);
        assertThat(user1.getReputation().getNotifyCount()).isEqualTo(1);
        assertThat(user1.getReputation().getTotalNotifyCount()).isEqualTo(1);
    }

//    @Test
//    @DisplayName("신고 3번 이상 => 3번 초과 신고 못함")
//    @WithUserDetails("user1")
//    void t12() throws Exception {
//        Member user1 = memberService.findByUsername("user1").get();
//        int userId = user1.getId();
//
//        ResultActions resultActions = null;
//
//        // 10번 반복
//        for (int i = 0; i < 10; i++) {
//            resultActions = mvc
//                    .perform(
//                            patch("/api/v1/members/%d/credit".formatted(userId))
//                    )
//                    .andDo(print());
//        }
//
//        // 마지막 결과만 검증
//        resultActions
//                .andExpect(handler().handlerType(MemberController.class))
//                .andExpect(handler().methodName("decrease"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.resultCode").value("403-1"))
//                .andExpect(jsonPath("$.msg").value("해당 회원에 대한 신고 횟수를 초과했습니다."));
//    }


    @Test
    @DisplayName("정지 계정 리뷰 쓰기 금지")
    @WithUserDetails("user6")
    void t14() throws Exception {
        ResultActions resultActions = null;

        resultActions = mvc
                .perform(
                        post("/api/v1/members/%d/review".formatted(3))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "star" : 5,
                                            "comment" : "친절합니다."
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("review"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-3"))
                .andExpect(jsonPath("$.msg").value("정지된 회원은 해당 기능을 사용할 수 없습니다."));

    }

    @Test
    @DisplayName("정상 계정 리뷰 쓰기 성공")
    @WithUserDetails("user3")
    void t15() throws Exception {
        ResultActions resultActions = null;

        resultActions = mvc
                .perform(
                        post("/api/v1/members/%d/review".formatted(3))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "star" : 5,
                                            "comment" : "친절합니다."
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("review"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("후기 작성이 완료되었습니다."));

    }

}
