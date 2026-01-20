package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.Reputation;
import com.back.domain.member.member.enums.Role;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.member.repository.ReputationRepository;
import com.back.domain.member.member.service.AuthTokenService;
import com.back.global.exception.ServiceException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final AuthTokenService authTokenService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReputationRepository reputationRepository;

    public long count() {
        return memberRepository.count();
    }

    public Member join(String username, String password, String nickname) {
        memberRepository
                .findByUsername(username)
                .ifPresent(_member -> {
                    throw new ServiceException("409-1", "이미 존재하는 아이디입니다.");
                });

        password = passwordEncoder.encode(password);

        Member member = new Member(username, password, nickname);
        if (username.startsWith("system") || username.startsWith("admin"))
            member.setRole(Role.ADMIN);
        else member.setRole(Role.USER);

        Reputation reputation = new Reputation(member, 50.0);
        reputationRepository.save(reputation);

        return memberRepository.save(member);
    }

    public Optional<Member> findByUsername(String username) {
        return memberRepository.findByUsername(username);
    }

    public Optional<Member> findByApiKey(String apiKey) {
        return memberRepository.findByApiKey(apiKey);
    }

    public String genAccessToken(Member member) {
        return authTokenService.genAccessToken(member);
    }

    public Map<String, Object> payload(String accessToken) {
        return authTokenService.payload(accessToken);
    }

    public Optional<Member> findById(int id) {
        return memberRepository.findById(id);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public void checkPassword(Member member, String password) {
        if (!passwordEncoder.matches(password, member.getPassword()))
            throw new ServiceException("401-1", "비밀번호가 일치하지 않습니다.");
    }

    public void modify(Member member, String nickname, String password, String newPassword, String checkPassword) {
        if (!passwordEncoder.matches(password, member.getPassword()))
            throw new ServiceException("401-1", "현재 비밀번호와 일치하지 않습니다.");

        if (!newPassword.equals(checkPassword))
            throw new ServiceException("401-1", "비밀번호가 일치하지 않습니다.");

        member.modify(nickname, passwordEncoder.encode(newPassword));
    }

    public void modifyNickname(Member member, String nickname) {
        member.modifyName(nickname);
    }

    public void modifyPassword(Member member, String password, String newPassword, String checkPassword) {
        if (!passwordEncoder.matches(password, member.getPassword()))
            throw new ServiceException("401-1", "현재 비밀번호와 일치하지 않습니다.");

        if (!newPassword.equals(checkPassword))
            throw new ServiceException("401-1", "비밀번호가 일치하지 않습니다.");

        member.modifyPassword(passwordEncoder.encode(newPassword));

    }
}