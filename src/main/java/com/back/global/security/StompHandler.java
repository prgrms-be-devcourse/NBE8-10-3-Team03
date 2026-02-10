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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Collections;
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
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        }

        // ③ 모든 WebSocket 메시지에 대해 SecurityContextHolder 설정
        // 이를 통해 Rq.getActor() 등 SecurityContextHolder를 참조하는 코드가
        // WebSocket 스코프에서도 정상 동작한다.
        propagateSecurityContext(accessor);

        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        // WebSocket 메시지 처리 완료 후 SecurityContext 클리어 (스레드 오염 방지)
        SecurityContextHolder.clearContext();
    }

    /**
     * CONNECT 단계: 토큰 검증 및 세션 유저 정보 저장
     *
     * ① 핸드셰이크 시점에 쿠키에서 추출해둔 인증 정보를 우선 사용하고,
     *    없으면 STOMP 헤더의 토큰으로 인증을 시도한다.
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        // 1. 핸드셰이크에서 저장된 인증 정보 확인 (쿠키 기반)
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Authentication handshakeAuth = (Authentication) sessionAttributes.get(
                    HttpHandshakeInterceptor.WS_AUTHENTICATION_KEY);
            if (handshakeAuth != null) {
                accessor.setUser(handshakeAuth);
                SecurityUser user = (SecurityUser) handshakeAuth.getPrincipal();
                log.info("STOMP Connected (핸드셰이크 인증): {}", user.getUsername());
                return;
            }
        }

        // 2. 핸드셰이크 인증이 없으면 STOMP 헤더의 토큰으로 인증
        String token = accessor.getFirstNativeHeader("token");

        if (token == null || !token.startsWith("Bearer ")) {
            log.error("STOMP Connect Error: Token missing or invalid");
            throw new RuntimeException("Unauthorized: Token Required");
        }

        token = token.substring(7);

        try {
            Map<String, Object> payload = memberService.payload(token);
            if (payload == null) throw new RuntimeException("Invalid Token Payload");

            SecurityUser user = createSecurityUser(payload);

            // 웹 소켓 세션에 인증 정보 저장 (이후 SUBSCRIBE 단계에서 활용)
            Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            accessor.setUser(auth);

            log.info("STOMP Connected (토큰 인증): {}", user.getUsername());
        } catch (Exception e) {
            log.error("STOMP Connect Auth Error: {}", e.getMessage());
            throw new RuntimeException("Unauthorized");
        }
    }

    /**
     * SUBSCRIBE 단계: 채팅방 접근 권한 검증
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        String dest = accessor.getDestination();
        if (dest == null) return;

        // 채팅방 관련 경로 매칭 (/sub/v1/chat/room/**)
        if (pathMatcher.match("/sub/v1/chat/room/**", dest)) {
            Authentication auth = (Authentication) accessor.getUser();

            if (auth == null || !(auth.getPrincipal() instanceof SecurityUser)) {
                throw new RuntimeException("Unauthorized: Login Required");
            }

            SecurityUser user = (SecurityUser) auth.getPrincipal();
            String roomId = extractRoomId(dest);

            // 채팅방 정보 조회
            ChatRoom room = chatRoomRepository.findByRoomIdAndDeletedFalse(roomId)
                    .orElseThrow(() -> new RuntimeException("ChatRoom Not Found"));

            // 권한 검증 (SecurityUser의 정보 활용)
            Member member = memberService.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Member Not Found"));

            if (!room.getSellerApiKey().equals(member.getApiKey()) &&
                    !room.getBuyerApiKey().equals(member.getApiKey())) {
                log.warn("Access Denied: User {} -> Room {}", user.getId(), roomId);
                throw new RuntimeException("Subscription not authorized");
            }

            log.info("STOMP Subscribed: User {} to Room {}", user.getUsername(), roomId);
        }

        // 개인 알림 토픽 (/sub/user/{userId}/notification)
        if (pathMatcher.match("/sub/user/*/notification", dest)) {
            Authentication auth = (Authentication) accessor.getUser();

            if (auth == null || !(auth.getPrincipal() instanceof SecurityUser)) {
                throw new RuntimeException("Unauthorized: Login Required");
            }

            SecurityUser user = (SecurityUser) auth.getPrincipal();

            // 경로에서 userId 추출
            String[] parts = dest.split("/");
            String targetUserId = parts[3]; // /sub/user/{userId}/notification

            // 본인의 알림 토픽만 구독 가능
            if (!String.valueOf(user.getId()).equals(targetUserId)) {
                log.warn("Access Denied: User {} tried to subscribe to User {}'s notification", user.getId(), targetUserId);
                throw new RuntimeException("Subscription not authorized");
            }

            log.info("STOMP Subscribed: User {} to personal notification", user.getUsername());
        }
    }

    /**
     * ② WebSocket 세션에 저장된 Authentication을 SecurityContextHolder에 설정.
     * 이를 통해 WebSocket 메시지 처리 중에도 Spring Security 컨텍스트를 공유할 수 있다.
     */
    private void propagateSecurityContext(StompHeaderAccessor accessor) {
        Authentication auth = (Authentication) accessor.getUser();
        if (auth != null) {
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }

    private SecurityUser createSecurityUser(Map<String, Object> payload) {
        int id = (int) payload.get("id");
        String username = (String) payload.get("username");
        String name = (String) payload.get("name");
        Role role = Role.from((String) payload.get("role"));

        return new SecurityUser(
                id, username, "", name, role,
                Collections.singletonList(new SimpleGrantedAuthority(role.name()))
        );
    }

    private String extractRoomId(String dest) {
        String[] parts = dest.split("/");
        // /read로 끝나는 경로는 뒤에서 2번째 파트가 roomId, 아니면 마지막 파트
        return dest.endsWith("/read") ? parts[parts.length - 2] : parts[parts.length - 1];
    }
}