package com.back.domain.member.member.controller

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.service.MemberService
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {
    @Autowired
    private lateinit var memberService: MemberService

    @Autowired
    private lateinit var mvc: MockMvc

    private fun bearer(apiKey: String): String = "Bearer $apiKey"

    private fun ensureMember(
        username: String,
        password: String = "Qw!9zK2m",
        nickname: String = username,
    ): Member = memberService.findByUsername(username)
        ?: memberService.join(username, password, nickname, null)


    @Test
    @DisplayName("회원가입")
    fun t1() {
        val resultActions = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "username": "usernew1",
                      "password": "Qw!9zK2m",
                      "nickname": "무명2"
                    }
                    """.trimIndent()
                )
        ).andDo(MockMvcResultHandlers.print())

        val member = memberService.findByUsername("usernew1")!!

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("201-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${member.nickname}님 환영합니다. 회원가입이 완료되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(member.id))
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 username")
    fun t2() {
        ensureMember("dupuser1", "Qw!9zK2m", "중복1")

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"dupuser1","password":"Qw!9zK2m","nickname":"중복2"}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("409-1"))
    }

    @Test
    @DisplayName("회원가입 실패 - validation")
    fun t3() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"u1","password":"123","nickname":"a"}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @DisplayName("로그인")
    fun t4() {
        val member = ensureMember("loginuser1", "Qw!9zK2m", "로그인유저")

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"loginuser1","password":"Qw!9zK2m"}""")
        ).andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("login"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${member.nickname}님 환영합니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.item.id").value(member.id))
            .andExpect(ResultMatcher { result: MvcResult ->
                val apiKeyCookie = result.response.getCookie("apiKey")
                Assertions.assertThat(apiKeyCookie!!.value).isEqualTo(member.apiKey)
                Assertions.assertThat(apiKeyCookie.path).isEqualTo("/")
                Assertions.assertThat(apiKeyCookie.isHttpOnly).isTrue()

                val accessTokenCookie = result.response.getCookie("accessToken")
                Assertions.assertThat(accessTokenCookie!!.value).isNotBlank()
                Assertions.assertThat(accessTokenCookie.path).isEqualTo("/")
                Assertions.assertThat(accessTokenCookie.isHttpOnly).isTrue()
            })
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 username")
    fun t5() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"not-exist-user","password":"Qw!9zK2m"}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("401-1"))
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    fun t6() {
        ensureMember("loginuser2", "Qw!9zK2m", "로그인유저2")

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"loginuser2","password":"wrong-password"}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(ResultMatcher { result ->
                Assertions.assertThat(result.response.status).isIn(401, 404)
                Assertions.assertThat(result.response.contentAsString)
                    .containsAnyOf("\"resultCode\":\"401-1\"", "\"resultCode\":\"404-1\"")
            })
    }

    @Test
    @DisplayName("내 정보")
    fun t7() {
        val member = ensureMember("meuser1", "Qw!9zK2m", "미유저")

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/members/me")
                .header("Authorization", bearer(member.apiKey!!))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("me"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(member.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(member.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(member.username))
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 미인증")
    fun t8() {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/members/me"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("401-1"))
    }

    @Test
    @DisplayName("내 정보, with apiKey Cookie")
    fun t9() {
        val member = ensureMember("meuser2", "Qw!9zK2m", "미유저2")

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/members/me")
                .cookie(Cookie("apiKey", member.apiKey))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("me"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(member.username))
    }

    @Test
    @DisplayName("로그아웃")
    fun t10() {
        mvc.perform(MockMvcRequestBuilders.delete("/api/v1/members/logout"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("logout"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그아웃 되었습니다."))
            .andExpect(ResultMatcher { result: MvcResult ->
                val apiKeyCookie = result.response.getCookie("apiKey")
                Assertions.assertThat(apiKeyCookie!!.value).isEmpty()
                Assertions.assertThat(apiKeyCookie.maxAge).isEqualTo(0)
                Assertions.assertThat(apiKeyCookie.path).isEqualTo("/")
                Assertions.assertThat(apiKeyCookie.isHttpOnly).isTrue()

                val accessTokenCookie = result.response.getCookie("accessToken")
                Assertions.assertThat(accessTokenCookie!!.value).isEmpty()
                Assertions.assertThat(accessTokenCookie.maxAge).isEqualTo(0)
                Assertions.assertThat(accessTokenCookie.path).isEqualTo("/")
                Assertions.assertThat(accessTokenCookie.isHttpOnly).isTrue()
            })
    }

    @Test
    @DisplayName("엑세스 토큰이 만료되었거나 유효하지 않다면 apiKey를 통해서 재발급")
    fun t11() {
        val member = ensureMember("refreshuser1", "Qw!9zK2m", "리프레시유저")

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/members/me")
                .header("Authorization", "Bearer ${member.apiKey} wrong-access-token")
        ).andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("me"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(ResultMatcher { result: MvcResult ->
                val accessTokenCookie = result.response.getCookie("accessToken")
                Assertions.assertThat(accessTokenCookie!!.value).isNotBlank()
                Assertions.assertThat(accessTokenCookie.path).isEqualTo("/")
                Assertions.assertThat(accessTokenCookie.isHttpOnly).isTrue()

                val headerAuthorization = result.response.getHeader("Authorization")
                Assertions.assertThat(headerAuthorization).isNotBlank()
                Assertions.assertThat(headerAuthorization).isEqualTo(accessTokenCookie.value)
            })
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer 형식이 아닐 때 오류")
    fun t12() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/members/me")
                .header("Authorization", "key")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("401-2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("Authorization 헤더가 Bearer 형식이 아닙니다."))
    }

    @Test
    @DisplayName("회원 닉네임 수정")
    fun t13() {
        val member = ensureMember("nickuser1", "Qw!9zK2m", "닉네임기존")

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/members/me/nickname")
                .header("Authorization", bearer(member.apiKey!!))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"new 무명"}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("modifyNickname"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("수정이 완료되었습니다."))
    }

    @Test
    @DisplayName("회원 닉네임 수정 실패 - validation")
    fun t14() {
        val member = ensureMember("nickuser2", "Qw!9zK2m", "닉네임기존2")

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/members/me/nickname")
                .header("Authorization", bearer(member.apiKey!!))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":""}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @DisplayName("비밀번호 수정")
    fun t15() {
        val member = ensureMember("pwuser1", "Qw!9zK2m", "비번유저")

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/members/me/password")
                .header("Authorization", bearer(member.apiKey!!))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password":"Qw!9zK2m","newPassword":"new1234","checkPassword":"new1234"}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("modifyPassword"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("수정이 완료되었습니다."))
    }

    @Test
    @DisplayName("비밀번호 수정 실패 - 현재 비밀번호 불일치")
    fun t16() {
        val member = ensureMember("pwuser2", "Qw!9zK2m", "비번유저2")

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/members/me/password")
                .header("Authorization", bearer(member.apiKey!!))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password":"wrong","newPassword":"new1234","checkPassword":"new1234"}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("401-1"))
    }

    @Test
    @DisplayName("비밀번호 수정 실패 - 새 비밀번호 확인 불일치")
    fun t17() {
        val member = ensureMember("pwuser3", "Qw!9zK2m", "비번유저3")

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/members/me/password")
                .header("Authorization", bearer(member.apiKey!!))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password":"Qw!9zK2m","newPassword":"new1234","checkPassword":"different1234"}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("401-1"))
    }

    @Test
    @DisplayName("신고 처리")
    fun t18() {
        val target = ensureMember("reportTarget1", "Qw!9zK2m", "신고대상")
        val reporter = ensureMember("reporter1", "Qw!9zK2m", "신고자")

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/members/${target.id}/credit")
                .header("Authorization", bearer(reporter.apiKey!!))
        ).andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("decrease"))
            .andExpect(ResultMatcher { result ->
                Assertions.assertThat(result.response.status).isIn(200, 404)
                Assertions.assertThat(result.response.contentAsString)
                    .containsAnyOf("\"resultCode\":\"200-1\"", "\"resultCode\":\"404-1\"")
            })
    }

    @Test
    @DisplayName("신고 실패 - 같은 대상 중복 신고")
    fun t19() {
        val target = ensureMember("reportTarget2", "Qw!9zK2m", "신고대상2")
        val reporter = ensureMember("reporter2", "Qw!9zK2m", "신고자2")

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/members/${target.id}/credit")
                .header("Authorization", bearer(reporter.apiKey!!))
        )
            .andExpect(ResultMatcher { result ->
                Assertions.assertThat(result.response.status).isIn(200, 404)
                Assertions.assertThat(result.response.contentAsString)
                    .containsAnyOf("\"resultCode\":\"200-1\"", "\"resultCode\":\"404-1\"")
            })

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/members/${target.id}/credit")
                .header("Authorization", bearer(reporter.apiKey!!))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(ResultMatcher { result ->
                Assertions.assertThat(result.response.status).isIn(400, 404)
                Assertions.assertThat(result.response.contentAsString)
                    .containsAnyOf("\"resultCode\":\"400-6\"", "\"resultCode\":\"404-1\"")
            })
    }

    @Test
    @DisplayName("정지 계정 리뷰 쓰기 금지")
    fun t20() {
        val seller = ensureMember("seller1", "Qw!9zK2m", "판매자")
        val suspendedReviewer = ensureMember("suspendedReviewer1", "Qw!9zK2m", "정지리뷰어")
        suspendedReviewer.suspend()

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/${seller.id}/review")
                .header("Authorization", bearer(suspendedReviewer.apiKey!!))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"star":5,"comment":"친절합니다."}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("review"))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("403-3"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("정지된 회원은 해당 기능을 사용할 수 없습니다."))
    }

    @Test
    @DisplayName("정상 계정 리뷰 쓰기 성공")
    fun t21() {
        val seller = ensureMember("seller2", "Qw!9zK2m", "판매자2")
        val reviewer = ensureMember("reviewer1", "Qw!9zK2m", "리뷰어1")

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/${seller.id}/review")
                .header("Authorization", bearer(reviewer.apiKey!!))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"star":5,"comment":"친절합니다."}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("review"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("201-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("후기 작성이 완료되었습니다."))
    }

    @Test
    @DisplayName("리뷰 목록 조회")
    fun t22() {
        val seller = ensureMember("seller3", "Qw!9zK2m", "판매자3")
        val reviewer = ensureMember("reviewer2", "Qw!9zK2m", "리뷰어2")

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/${seller.id}/review")
                .header("Authorization", bearer(reviewer.apiKey!!))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"star":4,"comment":"테스트 리뷰"}""")
        ).andExpect(MockMvcResultMatchers.status().isCreated())

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/members/${seller.id}/review")
                .header("Authorization", bearer(reviewer.apiKey!!))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getReviews"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
    }

    @Test
    @DisplayName("내 경매 목록 조회")
    fun t23() {
        val member = ensureMember("auctionlistuser1", "Qw!9zK2m", "경매목록유저")

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/members/me/auctions")
                .header("Authorization", bearer(member.apiKey!!))
                .param("page", "0")
                .param("size", "20")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getAuctionsById"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())
    }

    @Test
    @DisplayName("내 거래 목록 조회")
    fun t24() {
        val member = ensureMember("postlistuser1", "Qw!9zK2m", "거래목록유저")

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/members/me/posts")
                .header("Authorization", bearer(member.apiKey!!))
                .param("page", "0")
                .param("size", "10")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getPostsById"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-4"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    fun t25() {
        val member = ensureMember("withdrawuser1", "Qw!9zK2m", "탈퇴유저")

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/members/me/withdraw")
                .header("Authorization", bearer(member.apiKey!!))
        ).andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("withdraw"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("탈퇴가 정상적으로 처리되었습니다."))
            .andExpect(ResultMatcher { result: MvcResult ->
                val apiKeyCookie = result.response.getCookie("apiKey")
                Assertions.assertThat(apiKeyCookie!!.value).isEmpty()
                Assertions.assertThat(apiKeyCookie.maxAge).isZero()
                val accessTokenCookie = result.response.getCookie("accessToken")
                Assertions.assertThat(accessTokenCookie!!.value).isEmpty()
                Assertions.assertThat(accessTokenCookie.maxAge).isZero()
            })
    }

    @Test
    @DisplayName("프로필 사진 수정 성공")
    fun t26() {
        val member = ensureMember("profileuser1", "Qw!9zK2m", "프로필유저")
        val profileImg = MockMultipartFile("profileImg", "profile.jpg", "image/jpeg", "profile-bytes".toByteArray())

        mvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/members/me/profile")
                .file(profileImg)
                .header("Authorization", bearer(member.apiKey!!))
                .with { req ->
                    req.method = "PATCH"
                    req
                }
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("modifyProfile"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
    }

    @Test
    @DisplayName("프로필 사진 수정 실패 - 빈 파일")
    fun t27() {
        val member = ensureMember("profileuser2", "Qw!9zK2m", "프로필유저2")
        val profileImg = MockMultipartFile("profileImg", "", "image/jpeg", ByteArray(0))

        mvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/members/me/profile")
                .file(profileImg)
                .header("Authorization", bearer(member.apiKey!!))
                .with { req ->
                    req.method = "PATCH"
                    req
                }
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("modifyProfile"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
    }
}
