package com.back.domain.member.member.service;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.MemberStatus;
import com.back.domain.member.review.entity.Review;
import com.back.domain.member.review.repository.ReviewRepository;
import com.back.domain.member.reputation.entity.Reputation;
import com.back.domain.member.reputation.entity.ReputationEvent;
import com.back.domain.member.reputation.enums.EventType;
import com.back.domain.member.reputation.enums.RefType;
import com.back.domain.member.member.enums.Role;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.reputation.repository.ReputationEventRepository;
import com.back.domain.member.reputation.repository.ReputationRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ReviewRepository reviewRepository;

    public long count() {
        return memberRepository.count();
    }

    @Transactional
    public Member join(String username, String password, String nickname, String profileImgUrl) {
        memberRepository
                .findByUsername(username)
                .ifPresent(_member -> {
                    throw new ServiceException("409-1", "이미 존재하는 아이디입니다.");
                });

        // password가 null이면(OAuth) password에 null 저장
        password = (password != null && !password.isBlank()) ? passwordEncoder.encode(password) : null;

        Member member = new Member(username, password, nickname, profileImgUrl);

        // (임시) system이나 admin으로 가입시 ADMIN ROLE 부여
        if (username.startsWith("system") || username.startsWith("admin"))
            member.setRole(Role.ADMIN);
        else member.setRole(Role.USER);

        // (임시) 최초 신용도 50.0
        Reputation reputation = new Reputation(member, 50.0);
        reputationRepository.save(reputation);

        return memberRepository.save(member);
    }

    // OAuth 회원가입 / 로그인
    @Transactional
    public RsData<Member> modifyOrJoin(String username, String password, String nickname, String profileImgUrl) {
        Member member = findByUsername(username).orElse(null);

        if ( member == null ) {
            member = join(username, password, nickname, profileImgUrl);
            return new RsData<>("201-1", "회원가입이 완료되었습니다.", member);
        }

        modify(member, nickname, profileImgUrl);

        return new RsData<>("200-1", "회원 정보가 수정되었습니다.", member);
    }

    // OAuth 수정
    @Transactional
    private void modify(Member member, String nickname, String profileImgUrl) {
        member.modify(nickname, profileImgUrl);
    }

    // 로그인
    public void login(Member member, String password) {
        if (member.getStatus() == MemberStatus.BANNED)
            throw new ServiceException("403-3", "계정이 영구 정지되었습니다. 관리자에게 문의해주세요.");

        if (member.getStatus() == MemberStatus.WITHDRAWN)
            throw new ServiceException("403-4", "탈퇴한 계정입니다.");

        checkPassword(member, password);
    }

    // 회원 탈퇴
    @Transactional
    public void withdraw(Member actor) {
        Member member = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new ServiceException("400-1", "이미 탈퇴한 회원입니다.");
        }

        member.withdraw();
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


    @Transactional
    public void modifyNickname(Member member, String nickname) {
        member.modifyName(nickname);
    }

    @Transactional
    public void modifyPassword(Member member, String password, String newPassword, String checkPassword) {
        if (!passwordEncoder.matches(password, member.getPassword()))
            throw new ServiceException("401-1", "현재 비밀번호와 일치하지 않습니다.");

        if (!newPassword.equals(checkPassword))
            throw new ServiceException("401-1", "비밀번호가 일치하지 않습니다.");

        member.modifyPassword(passwordEncoder.encode(newPassword));

    }


    // 신고에 의한 신용도 감소
    @Transactional
    public void decreaseByNofiy(Member member, Member reporter) {
        int targetId = member.getId();
        int reporterId = reporter.getId();
        int count = eventRepository.countByTargetIdAndReporterId(targetId, reporterId);

        // A 회원 대상 B 회원의 최대 신고 횟수 3
        if (count >= 3) {
            throw new ServiceException("403-1", "해당 회원에 대한 신고 횟수를 초과했습니다.");
        }

        ReputationEvent event = new ReputationEvent(member, EventType.NOTIFY, RefType.DEAL, reporter);
        eventRepository.save(event);

        Reputation reputation = reputationRepository.findById(targetId).get();

        // 이미 정지/탈퇴/영구정지 상태면 신고 누적 X
        if (member.getStatus() == MemberStatus.SUSPENDED || member.getStatus() == MemberStatus.BANNED || member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new ServiceException("400-2", "해당 회원은 정지된 회원입니다.");
        }

        // 신고 누적
        reputation.increaseNotify();

        // 신고 10회 누적 시마다 신용도 감소
        if (reputation.getNotifyCount() % 10 == 0) {
            reputation.decrease();
        }

        // 신고 누적 100회 -> 일주일간 정지
        if (reputation.getNotifyCount() >= 100) {
            member.suspend();
            reputation.setNotifyCount(0);
        }

        // 신고 누적 10000회 이상 -> 영구 정지
        if (reputation.getTotalNotifyCount() >= 10000) {
            member.banned();
        }
    }

    // 경매 취소 시 신용도 감소 (if 입찰 O)
    @Transactional
    public void decreaseByCancel(int auctionId, int actorId) {
        Auction auction = auctionRepository.findById(auctionId).get();
        Member seller = memberRepository.findById(actorId).get();

        // if 입찰 O
        if (auction.getBidCount() > 0) {
            Reputation reputation = reputationRepository.findById(seller.getId()).get();

            // 증감 계산
            double before = reputation.getScore();
            reputation.decrease();
            double after = reputation.getScore();

            ReputationEvent event = new ReputationEvent(seller, EventType.CANCEL, RefType.AUCTION, auctionId, Math.abs(before - after), null);
            eventRepository.save(event);
        }
    }

    // 낙찰 & 거래 완료 시 신용도 증가
    @Transactional
    public void increaseByDeal(int dealId) {
        Auction auction = auctionRepository.findById(dealId).get();
        Member seller = memberRepository.findById(auction.getSeller().getId()).get();

        Reputation reputation = reputationRepository.findById(seller.getId()).get();
        // 증감 계산
        double before = reputation.getScore();
        reputation.increase();
        double after = reputation.getScore();

        ReputationEvent event = new ReputationEvent(seller, EventType.CANCEL, RefType.AUCTION, dealId, Math.abs(before - after), null);
        eventRepository.save(event);
    }


    // 리뷰 생성
    @Transactional
    public Review createReview(int star, String msg, Member member, int reviewerId) {
        if(findById(reviewerId).get().getStatus() == MemberStatus.SUSPENDED) {
            throw new ServiceException("403-3", "정지된 회원은 해당 기능을 사용할 수 없습니다.");
        }
        Review review = new Review(member, star, msg, reviewerId);
        return reviewRepository.save(review);
    }
}