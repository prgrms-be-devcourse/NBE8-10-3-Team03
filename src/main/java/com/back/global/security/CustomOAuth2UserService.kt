package com.back.global.security

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.service.MemberService
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CustomOAuth2UserService(
    private val memberService: MemberService
) : DefaultOAuth2UserService() {

    // 카카오톡 로그인이 성공할 때 마다 이 함수가 실행된다.
    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)

        val oauthUserId = oAuth2User.name
        val providerTypeCode = userRequest.clientRegistration.registrationId.uppercase(Locale.getDefault())

        val attributes = oAuth2User.attributes
        val attributesProperties = attributes.get("properties") as MutableMap<String, Any>

        val userNicknameAttributeName = "nickname"
        val profileImgUrlAttributeName = "profile_image"

        val nickname = attributesProperties.get(userNicknameAttributeName) as String
        val profileImgUrl = attributesProperties.get(profileImgUrlAttributeName) as String?
        val username = providerTypeCode + "__${oauthUserId}"
        val password = ""
        val member: Member = memberService.modifyOrJoin(username, password, nickname, profileImgUrl).data!!

        return SecurityUser(
            member.getId(),
            member.username,
            member.password,
            member.nickname,
            member.role,
            member.authorities
        )
    }
}
