package com.back.global.security;

import com.back.domain.chat.chat.entity.ChatRoom;
import com.back.domain.chat.chat.repository.ChatRoomRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.Role;
import com.back.domain.member.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final MemberService memberService;
    private final ChatRoomRepository chatRoomRepository;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // accessor를 이용해 헤더 정보만 빼옴
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // CONNECT 시 토큰 검증 및 유저 정보 셋팅
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("token");

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);

                // 복호화
                try {
                    Map<String, Object> payload = memberService.payload(token);

                    if (payload != null) {
                        int id = (int) payload.get("id");
                        String username = (String) payload.get("username");
                        String name = (String) payload.get("name");
                        String roleStr = (String) payload.get("role");
                        Role role = Role.from(roleStr);

                        List<SimpleGrantedAuthority> authorities =
                                Collections.singletonList(new SimpleGrantedAuthority(role.name()));

                        SecurityUser user = new SecurityUser(
                                id,
                                username,
                                "", // 비밀번호는 필요 없음
                                name,
                                role,
                                authorities
                        );

                        // 웹 소켓 세션에 유저 정보 저장
                        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        accessor.setUser(auth);

                        log.info("STOMP Connected: {}", username);
                    }
                } catch (Exception e) {
                    log.error("STOMP Connect Error: {}", e.getMessage());
                    // 인증 실패 예외 발생
                    throw new RuntimeException("Unauthorized");
                }
            }
        }

        // SUBSCRIBE 시 접근 권한 확인 로직
        else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            // 목적지 추출 Ex: /sub/v1/chat/room/{roomId}
            String dest = accessor.getDestination();
            String chatRoomPattern = "/sub/v1/chat/room/{roomId}";

            // 구독 경로가 채팅방인지 체크
            if (dest != null && pathMatcher.match(chatRoomPattern, dest)) {

                Authentication auth = (Authentication) accessor.getUser();

                // 로그인하지 않은 사용자 차단
                if (auth == null || !(auth.getPrincipal() instanceof SecurityUser)) {
                    throw new RuntimeException("Unauthorized: Login Required for Chat");
                }

                SecurityUser user = (SecurityUser) auth.getPrincipal();

                // RoomId 파싱 로직
                String roomId = dest.substring(dest.lastIndexOf("/") + 1);

                ChatRoom room = chatRoomRepository.findByRoomId(roomId)
                        .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

                Member member = memberService.findById(user.getId())
                        .orElseThrow(() -> new RuntimeException("Member not found"));

                // 권한 검증
                if (!room.getSellerId().equals(member.getApiKey()) && !room.getBuyerId().equals(member.getApiKey())) {
                    log.warn("사용자(ID:{})가 권한 없는 채팅방({}) 구독 시도", user.getId(), roomId);
                    throw new RuntimeException("Subscription not authorized");
                }
            }
        }
        return message;
    }
}
