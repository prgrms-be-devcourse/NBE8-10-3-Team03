package com.back.domain.member.member.service;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.auction.auction.service.FileStorageService;
import com.back.domain.image.image.entity.Image;
import com.back.domain.image.image.repository.ImageRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.MemberStatus;
import com.back.domain.member.reputation.entity.Report;
import com.back.domain.member.reputation.repository.ReportRepository;
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
import com.back.global.audit.enums.AuditType;
import com.back.global.audit.service.SecurityAuditService;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

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
    private final ReportRepository reportRepository;
    private final LoginFailService loginFailService;
    private final SecurityAuditService auditService;
    private final HttpServletRequest servletRequest;
    private final FileStorageService fileStorageService;
    private final ImageRepository imageRepository;

    public long count() {
        return memberRepository.count();
    }

    @Transactional
    public Member join(String username, String password, String name, String profileImgUrl) {
        String nickname = name.trim();
        memberRepository
                .findByUsername(username)
                .ifPresent(_member -> {
                    throw new ServiceException("409-1", "이미 존재하는 아이디입니다.");
                });
        memberRepository.findByNickname(nickname)
                .ifPresent(_member -> {
                    throw new ServiceException("409-1", "%s(은)는 사용중인 닉네임입니다.".formatted(nickname));
                });

        if (password != null && !password.isBlank()) {
            passwordValidation(username, password);
            passwordEncoder.encode(password);
        }
        else {
            password = null;
        }

        Member member = new Member(username, password, nickname, profileImgUrl);

        // (임시) system이나 admin으로 가입시 ADMIN ROLE 부여
        if (username.equals("system") || username.equals("admin"))
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
    @Transactional
    public void login(Member member, String password) {
        if (member.getStatus() == MemberStatus.BANNED)
            throw new ServiceException("403-3", "계정이 영구 정지되었습니다. 관리자에게 문의해주세요.");

        if (member.getStatus() == MemberStatus.WITHDRAWN)
            throw new ServiceException("403-4", "탈퇴한 계정입니다.");

        // 계정 잠김이 만료되었는지 확인
        member.unlockIfExpired();

        // 계정이 잠겨있으면 에러
        if (member.isLocked()) {
            if (member.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new ServiceException("400-5", "계정이 일시적으로 잠겼습니다.");
            }
        }

        if (password != null) {
            checkPassword(member, password);
        }
        member.resetFailCount();
    }

    @Transactional
    public void checkPassword(Member member, String password) {
        LocalDateTime now = LocalDateTime.now();
        // 패스워드가 일치하지 않으면
        if (!passwordEncoder.matches(password, member.getPassword())) {
            loginFailService.record(member.getId(), now);

            if (member.getLoginFailCount() >= 5) {
                loginFailService.lock(member.getId(), now);
                auditService.log(member.getId(), AuditType.LOCK, servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent"));
                throw new ServiceException("401-1", "비밀번호 입력 횟수를 초과하였습니다. 10분 뒤에 다시 시도해주세요.");
            }

            throw new ServiceException("401-1", "비밀번호가 일치하지 않습니다.");
        }
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
        auditService.log(member.getId(), AuditType.WITHDRAW, servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent"));
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
    public void decreaseByNofiy(Member target, Member reporter) {
        int targetId = target.getId();
        int reporterId = reporter.getId();

        int count = reportRepository.countByReporterIdAndCreateDateAfter(reporterId, LocalDateTime.now().minusDays(1));

        // 신고는 하루에 3번만 가능
        if (count >= 3) {
            throw new ServiceException("400-6", "신고는 하루에 3번만 가능합니다.");
        }

        boolean exists = reportRepository.existsByReporterAndTargetAndCreateDateAfter(
                        reporter,
                        target,
                        LocalDateTime.now().minusDays(1)
                );

        // A 회원 대상 B 회원의 최대 신고 횟수 하루에 1번 제한
        if (exists) {
            throw new ServiceException("400-6", "이미 신고한 회원입니다.");
        }

        Report report = new Report(target, reporter);
        reportRepository.save(report);


        Reputation reputation = reputationRepository.findById(targetId).get();

        // 이미 정지/탈퇴/영구정지 상태면 신고 누적 X
        if (target.getStatus() == MemberStatus.SUSPENDED || target.getStatus() == MemberStatus.BANNED)
            throw new ServiceException("400-2", "해당 회원은 정지된 회원입니다.");

        if (target.getStatus() == MemberStatus.WITHDRAWN)
            throw new ServiceException("400-3", "해당 회원은 탈퇴한 회원입니다.");

        // 신고 누적
        reputation.increaseNotify();

        // 신고 10회 누적 시마다 신용도 감소
        if (reputation.getNotifyCount() % 10 == 0) {
            reputation.decrease();
        }

        // 신고 누적 100회 -> 일주일간 정지
        if (reputation.getNotifyCount() >= 100) {
            target.suspend();
            reputation.setNotifyCount(0);
        }

        // 신고 누적 10000회 이상 -> 영구 정지
        if (reputation.getTotalNotifyCount() >= 10000) {
            target.banned();
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

            ReputationEvent event = new ReputationEvent(seller, EventType.CANCEL, RefType.AUCTION, auctionId, Math.abs(before - after));
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

        ReputationEvent event = new ReputationEvent(seller, EventType.CANCEL, RefType.AUCTION, dealId, Math.abs(before - after));
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


    public void passwordValidation(String username, String password) {
        if (password == null) return;

        // 길이 검증 (이미 @Size로 체크되지만 명시적으로)
        if (password.length() < 8 || password.length() > 20) {
            throw new ServiceException("400-1", "비밀번호는 8-20자여야 합니다");
        }

        // 복잡도 검증 (영문 대/소문자, 숫자, 특수문자 중 3가지 이상)
        int complexityCount = 0;
        if (Pattern.compile("[a-z]").matcher(password).find()) complexityCount++;
        if (Pattern.compile("[A-Z]").matcher(password).find()) complexityCount++;
        if (Pattern.compile("[0-9]").matcher(password).find()) complexityCount++;
        if (Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]").matcher(password).find()) complexityCount++;

        if (complexityCount < 3) {
            throw new ServiceException("400-1", "비밀번호는 영문 대/소문자, 숫자, 특수문자 중 3가지 이상 조합이어야 합니다");
        }

        // 연속된 문자 검증
        if (hasConsecutiveChars(password)) {
            throw new ServiceException("400-1", "연속된 문자 또는 숫자 3개 이상 사용할 수 없습니다");
        }

        // 동일 문자 반복 검증
        if (Pattern.compile("(.)\\1\\1").matcher(password).find()) {
            throw new ServiceException("400-1", "동일한 문자를 3번 이상 연속 사용할 수 없습니다");
        }

        // 아이디 포함 검증
        if (username != null && !username.isEmpty() &&
                password.toLowerCase().contains(username.toLowerCase())) {
            throw new ServiceException("400-1", "비밀번호에 아이디를 포함할 수 없습니다");
        }
    }

    private boolean hasConsecutiveChars(String password) {
        String lowerPassword = password.toLowerCase();

        // 연속된 숫자 체크
        String[] consecutiveNumbers = {
                "012", "123", "234", "345", "456", "567", "678", "789", "890"
        };
        for (String seq : consecutiveNumbers) {
            if (lowerPassword.contains(seq)) return true;
        }

        // 연속된 알파벳 체크
        for (int i = 0; i < lowerPassword.length() - 2; i++) {
            char c1 = lowerPassword.charAt(i);
            char c2 = lowerPassword.charAt(i + 1);
            char c3 = lowerPassword.charAt(i + 2);

            if (Character.isLetter(c1) && Character.isLetter(c2) && Character.isLetter(c3)) {
                if (c2 == c1 + 1 && c3 == c2 + 1) {
                    return true;
                }
            }
        }

        return false;
    }

    // 프로필 사진 변경
    @Transactional
    public void modifyProfile(int memberId, MultipartFile profileImg) {
        if (profileImg == null || profileImg.isEmpty()) {
            throw new ServiceException("400-1", "업로드할 이미지 파일이 없습니다.");
        }

        // 트랜잭션 내에서 managed entity를 조회해야 dirty checking이 동작함
        Member managedMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));

        String imageUrl = fileStorageService.storeFile(profileImg);

        imageRepository.save(new Image(imageUrl));

        managedMember.modify(managedMember.getNickname(), imageUrl);
    }

}