package com.back.domain.member.member.service;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.Reputation;
import com.back.domain.member.member.entity.ReputationEvent;
import com.back.domain.member.member.enums.EventType;
import com.back.domain.member.member.enums.RefType;
import com.back.domain.member.member.enums.Role;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.member.repository.ReputationEventRepository;
import com.back.domain.member.member.repository.ReputationRepository;
import com.back.domain.member.member.service.AuthTokenService;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
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
    private final ReputationEventRepository eventRepository;
    private final AuctionRepository auctionRepository;

    public long count() {
        return memberRepository.count();
    }

    public Member join(String username, String password, String nickname, String profileImgUrl) {
        memberRepository
                .findByUsername(username)
                .ifPresent(_member -> {
                    throw new ServiceException("409-1", "이미 존재하는 아이디입니다.");
                });

        password = (password != null && !password.isBlank()) ? passwordEncoder.encode(password) : null;

        Member member = new Member(username, password, nickname, profileImgUrl);
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

    public void decreaseByNofiy(Member member) {
        int userId = member.getId();
        ReputationEvent event = new ReputationEvent(member, EventType.NOTIFY, RefType.DEAL);
        eventRepository.save(event);

        int caution = eventRepository.findByUserIdWithSum(userId, EventType.NOTIFY) / 10;

        Reputation reputation = reputationRepository.findById(userId).get();
        int nowCaution = reputation.getCaution();
        if (nowCaution < caution) {
            reputation.update(caution);
            reputation.decrease();
        }
    }

    // 경매 취소 시 신용도 감소 (if 입찰 O)
    public void decreaseByCancel(int auctionId) {
        Auction auction = auctionRepository.findById(auctionId).get();
        Member seller = memberRepository.findById(auction.getSeller().getId()).get();

        if (auction.getBidCount() > 0) {
            Reputation reputation = reputationRepository.findById(seller.getId()).get();
            double before = reputation.getScore();
            reputation.decrease();
            double after = reputation.getScore();
            double delta = Math.abs(before - after);
            ReputationEvent event = new ReputationEvent(seller, EventType.CANCEL, RefType.AUCTION, auctionId, delta);
            eventRepository.save(event);
        }
    }

    // 낙찰 & 거래 완료 시 신용도 증가
    public void increaseByDeal(int dealId) {
        Auction auction = auctionRepository.findById(dealId).get();
        Member seller = memberRepository.findById(auction.getSeller().getId()).get();

        Reputation reputation = reputationRepository.findById(seller.getId()).get();
        double before = reputation.getScore();
        reputation.increase();
        double after = reputation.getScore();
        double delta = Math.abs(before - after);
        ReputationEvent event = new ReputationEvent(seller, EventType.CANCEL, RefType.AUCTION, dealId, delta);
        eventRepository.save(event);
    }

    public RsData<Member> modifyOrJoin(String username, String password, String nickname, String profileImgUrl) {
        Member member = findByUsername(username).orElse(null);

        if ( member == null ) {
            member = join(username, password, nickname, profileImgUrl);
            return new RsData<>("201-1", "회원가입이 완료되었습니다.", member);
        }

        modify(member, nickname, profileImgUrl);

        return new RsData<>("200-1", "회원 정보가 수정되었습니다.", member);
    }

    private void modify(Member member, String nickname, String profileImgUrl) {
        member.modify(nickname, profileImgUrl);
    }
}