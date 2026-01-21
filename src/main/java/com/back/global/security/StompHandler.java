package com.back.global.security;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.Role;
import com.back.domain.member.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final MemberService memberService;

    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT == accessor.getCommand()) {
            String token = accessor.getFirstNativeHeader("token");

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);

                Map<String, Object> payload = memberService.payload(token);

                if (payload != null) {
                    int id = (int) payload.get("id");
                    String username = (String) payload.get("username");
                    String name = (String) payload.get("name");
                    String role = (String) payload.get("role");

                    Member member = new Member(id, username, name, Role.from(role));

                    SecurityUser user = new SecurityUser(
                            member.getId(), member.getUsername(), "", member.getName(), member.getRole(), member.getAuthorities()
                    );

                    Authentication auth = new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities() );
                }
            }
        }
        return message;
    }
}
