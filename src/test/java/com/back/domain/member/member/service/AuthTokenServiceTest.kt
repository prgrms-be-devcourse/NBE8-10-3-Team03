package com.back.domain.member.member.service

import com.back.domain.member.member.enums.Role
import com.back.standard.util.Ut
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.util.*
import com.back.domain.member.member.entity.Member

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthTokenServiceTest {
    @Autowired
    private lateinit var memberService: MemberService

    @Autowired
    private lateinit var authTokenService: AuthTokenService

    @Value("\${custom.jwt.secretKey}")
    private lateinit var jwtSecretKey: String

    @Value("\${custom.accessToken.expirationSeconds}")
    private val accessTokenExpirationSeconds = 0

    private fun ensureMember(username: String, nickname: String): com.back.domain.member.member.entity.Member =
        memberService.findByUsername(username).orElseGet {
            memberService.join(username, "Qw!9zK2m", nickname, null)
        }

    private fun createJwt(payload: Map<String, Any?>): String {
        val keyBytes = jwtSecretKey.toByteArray(StandardCharsets.UTF_8)
        val secretKey = Keys.hmacShaKeyFor(keyBytes)
        val issuedAt = Date()
        val expiration = Date(issuedAt.time + 1000L * accessTokenExpirationSeconds)

        return Jwts.builder()
            .claims(payload)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    @Test
    @DisplayName("authTokenService 서비스가 존재한다.")
    fun t1() {
        Assertions.assertThat(authTokenService).isNotNull()
    }

    @Test
    @DisplayName("jjwt 최신 방식으로 JWT 생성, {name=\"Paul\", age=23}")
    fun t2() {
        // 토큰 만료기간: 1년
        val expireMillis = 1000L * accessTokenExpirationSeconds

        val keyBytes = jwtSecretKey.toByteArray(StandardCharsets.UTF_8)
        val secretKey = Keys.hmacShaKeyFor(keyBytes)

        // 발행 시간과 만료 시간 설정
        val issuedAt = Date()
        val expiration = Date(issuedAt.getTime() + expireMillis)

        val payload = mapOf(
            "name" to "Paul",
            "age" to 23,
            "role" to Role.USER.name
        )

        val jwt = Jwts.builder()
            .claims(payload) // 내용
            .issuedAt(issuedAt) // 생성날짜
            .expiration(expiration) // 만료날짜
            .signWith(secretKey) // 키 서명
            .compact()

        Assertions.assertThat(jwt).isNotBlank()

        println("jwt = " + jwt)

        // 키가 유효한지 테스트
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(jwt)
            .getPayload()

        val parsedPayload: MutableMap<String, Any?> = LinkedHashMap(claims)

        Assertions.assertThat(parsedPayload)
            .containsAllEntriesOf(payload)
    }

    @Test
    @DisplayName("Ut.jwt.toString 를 통해서 JWT 생성, {name=\"Paul\", age=23}")
    fun t3() {
        val payload = mutableMapOf<String, Any>("name" to "Paul", "age" to 23, "role" to Role.USER.name)

        val jwt = Ut.jwt.toString(
            jwtSecretKey,
            accessTokenExpirationSeconds,
            payload
        )

        Assertions.assertThat(jwt).isNotBlank()

        Assertions.assertThat(
            Ut.jwt.isValid(jwtSecretKey, jwt)
        )
            .isTrue()

        val parsedPayload: MutableMap<String, Any>? = Ut.jwt.payload(jwtSecretKey, jwt)

        Assertions.assertThat(parsedPayload).containsAllEntriesOf(payload)
    }

    @Test
    @DisplayName("authTokenService.genAccessToken(member);")
    fun t4() {
        val memberUser1 = ensureMember("tokenuser1", "토큰유저1")

        val accessToken = authTokenService.genAccessToken(memberUser1)

        Assertions.assertThat(accessToken).isNotBlank()

        println("accessToken = " + accessToken)

        val parsedPayload: Map<String, Any>? = authTokenService.payload(accessToken)

        Assertions.assertThat(parsedPayload)
            .containsAllEntriesOf(
                mapOf(
                    "id" to memberUser1.id,
                    "username" to memberUser1.username,
                    "name" to memberUser1.nickname,
                    "role" to memberUser1.role!!.name
                )
            )
    }

    @Test
    @DisplayName("payload(null) 은 null 을 반환한다.")
    fun t5() {
        val parsedPayload = authTokenService.payload(null)

        Assertions.assertThat(parsedPayload).isNull()
    }

    @Test
    @DisplayName("유효하지 않은 accessToken 문자열이면 payload 는 null 을 반환한다.")
    fun t6() {
        val parsedPayload = authTokenService.payload("not-a-jwt-token")

        Assertions.assertThat(parsedPayload).isNull()
    }

    @Test
    @DisplayName("다른 secret 으로 서명된 토큰은 payload 파싱에 실패한다.")
    fun t7() {
        val tokenFromAnotherSecret = Ut.jwt.toString(
            "x".repeat(64),
            accessTokenExpirationSeconds,
            mutableMapOf(
                "id" to 1,
                "username" to "user1",
                "name" to "유저1",
                "role" to Role.USER.name
            )
        )

        val parsedPayload = authTokenService.payload(tokenFromAnotherSecret)

        Assertions.assertThat(parsedPayload).isNull()
    }

    @Test
    @DisplayName("genAccessToken 으로 생성한 토큰은 Ut.jwt.isValid 기준으로 유효하다.")
    fun t8() {
        val memberUser1 = ensureMember("tokenuser2", "토큰유저2")
        val accessToken = authTokenService.genAccessToken(memberUser1)

        Assertions.assertThat(Ut.jwt.isValid(jwtSecretKey, accessToken)).isTrue()
    }

    @Test
    @DisplayName("id 클레임이 누락된 토큰은 payload 변환에서 예외가 발생한다.")
    fun t9() {
        val tokenWithoutId = createJwt(
            mapOf(
                "username" to "user1",
                "name" to "유저1",
                "role" to Role.USER.name
            )
        )

        assertThatThrownBy { authTokenService.payload(tokenWithoutId) }
            .isInstanceOf(NullPointerException::class.java)
    }

    @Test
    @DisplayName("id 타입이 Int 가 아니면 payload 변환에서 예외가 발생한다.")
    fun t10() {
        val tokenWithLongId = createJwt(
            mapOf(
                "id" to 2147483648L,
                "username" to "user1",
                "name" to "유저1",
                "role" to Role.USER.name
            )
        )

        assertThatThrownBy { authTokenService.payload(tokenWithLongId) }
            .isInstanceOfAny(ClassCastException::class.java, ArithmeticException::class.java)
    }

    @Test
    @DisplayName("role 클레임이 누락된 토큰은 payload 변환에서 예외가 발생한다.")
    fun t11() {
        val tokenWithoutRole = createJwt(
            mapOf(
                "id" to 1,
                "username" to "user1",
                "name" to "유저1"
            )
        )

        assertThatThrownBy { authTokenService.payload(tokenWithoutRole) }
            .isInstanceOf(NullPointerException::class.java)
    }

    @Test
    @DisplayName("서로 다른 회원의 accessToken payload 는 id, username 이 다르다.")
    fun t12() {
        val memberUser1 = ensureMember("tokenuser3", "토큰유저3")
        val memberUser2 = ensureMember("tokenuser4", "토큰유저4")

        val token1 = authTokenService.genAccessToken(memberUser1)
        val token2 = authTokenService.genAccessToken(memberUser2)

        val payload1 = authTokenService.payload(token1)
        val payload2 = authTokenService.payload(token2)

        Assertions.assertThat(payload1).isNotNull()
        Assertions.assertThat(payload2).isNotNull()
        Assertions.assertThat(payload1!!["id"]).isNotEqualTo(payload2!!["id"])
        Assertions.assertThat(payload1["username"]).isNotEqualTo(payload2["username"])
    }

    @Test
    @DisplayName("만료된 토큰은 payload 파싱 시 null 을 반환한다.")
    fun t13() {
        val expiredToken = Ut.jwt.toString(
            jwtSecretKey,
            -1,
            mutableMapOf(
                "id" to 1,
                "username" to "expired-user",
                "name" to "만료유저",
                "role" to Role.USER.name
            )
        )

        Assertions.assertThat(authTokenService.payload(expiredToken)).isNull()
    }

    @Test
    @DisplayName("member.role 이 null 이면 accessToken 생성 시 예외가 발생한다.")
    fun t14() {
        val memberWithoutRole = Member(1, "norole-user", "권한없음")

        assertThatThrownBy { authTokenService.genAccessToken(memberWithoutRole) }
            .isInstanceOf(NullPointerException::class.java)
    }
}
