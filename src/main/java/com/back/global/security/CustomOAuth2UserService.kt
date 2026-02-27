package com.back.global.security

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.service.MemberService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
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
        val username = "${providerTypeCode}__${oauthUserId}"

        // provider별 사용자 추가 정보는 "properties" 하위 맵에 담겨 들어온다.
        val attributesProperties = oAuth2User.attributes["properties"] as? Map<*, *>
            ?: error("OAuth2 properties is missing.")

        val userNicknameAttributeName = "nickname"
        val profileImgUrlAttributeName = "profile_image"

        val nickname = attributesProperties[userNicknameAttributeName] as? String
            ?: error("OAuth2 nickname is missing.")
        val profileImgUrl = attributesProperties[profileImgUrlAttributeName] as? String
        val member: Member = memberService.modifyOrJoin(
            username = username,
            password = "",
            nickname = nickname,
            profileImgUrl = profileImgUrl,
        ).data ?: error("modifyOrJoin returned null member.")

        return SecurityUser(
            member.id,
            member.username,
            member.password,
            member.nickname,
            member.role,
            member.authorities
        )
    }
}
