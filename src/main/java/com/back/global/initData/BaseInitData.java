package com.back.global.initData;

import com.back.domain.auction.auction.dto.request.AuctionCreateRequest;
import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.auction.auction.service.AuctionService;
import com.back.domain.bid.bid.dto.request.BidCreateRequest;
import com.back.domain.bid.bid.service.BidService;
import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.service.port.CategoryPort;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.MemberStatus;
import com.back.domain.member.member.enums.Role;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.member.reputation.entity.Reputation;
import com.back.domain.member.reputation.repository.ReputationRepository;
import com.back.domain.member.review.entity.Review;
import com.back.domain.member.review.repository.ReviewRepository;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.entity.PostStatus;
import com.back.global.app.AppConfig;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import com.back.domain.post.post.repository.PostRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BaseInitData {
    @Autowired
    @Lazy
    private BaseInitData self;
    private final MemberService memberService;
    private final CategoryPort categoryPort;
    private final AuctionRepository auctionRepository;
    private final ReputationRepository reputationRepository;
    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final AuctionService auctionService;
    private final BidService bidService;
    private final PostRepository postRepository;

    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Bean
    ApplicationRunner baseInitDataApplicationRunner() {
        return args -> {
            self.work1();
            self.work2();
            self.work3();
            self.work4();
            self.work5();
        };
    }

    @Transactional
    public void work1() {
        if (memberService.count() > 0) return;

        // 시스템 계정
        Member memberSystem = new Member ("system", passwordEncoder.encode("1234"), "시스템", Role.ADMIN,null);
        if (AppConfig.isNotProd()) memberSystem.modifyApiKey(memberSystem.getUsername());
        memberRepository.save(memberSystem);
        reputationRepository.save(new Reputation(memberSystem, 50.0));

        // 관리자 계정
        Member memberAdmin = new Member("admin", passwordEncoder.encode("1234"), "관리자", Role.ADMIN,null);
        if (AppConfig.isNotProd()) memberAdmin.modifyApiKey(memberAdmin.getUsername());
        memberRepository.save(memberAdmin);
        reputationRepository.save(new Reputation(memberAdmin, 50.0));

        //  서비스 시연용: 일반 유저 5명 생성
        log.info(" 서비스 시연 - 일반 유저 생성 시작: 5명");

        for (int i = 1; i <= 5; i++) {
            String username = "user" + i;
            String nickname = "유저" + i;

            Member member = new Member(username, passwordEncoder.encode("1234"), nickname, Role.USER, null);
            if (AppConfig.isNotProd()) member.modifyApiKey(member.getUsername());
            memberRepository.save(member);

            // 신용도 랜덤 설정 (40.0 ~ 100.0)
            double reputationScore = 40.0 + (Math.random() * 60.0);
            reputationRepository.save(new Reputation(member, reputationScore));
        }

        entityManager.flush();
        entityManager.clear();
        log.info("서비스 시연 - 일반 유저 생성 완료: 총 5명");

        // 정지 회원 (user6)
        Member memberSuspended = new Member("user6", passwordEncoder.encode("1234"), "정지유저", Role.USER, null);
        if (AppConfig.isNotProd()) memberSuspended.modifyApiKey(memberSuspended.getUsername());
        memberSuspended.setStatus(MemberStatus.SUSPENDED);
        memberSuspended.setSuspendAt(LocalDateTime.now().minusDays(3));
        memberRepository.save(memberSuspended);
        reputationRepository.save(new Reputation(memberSuspended, 50.0));

        // 영구 정지 회원 (user7)
        Member memberBanned = new Member("user7", passwordEncoder.encode("1234"), "영구정지유저", Role.USER, null);
        if (AppConfig.isNotProd()) memberBanned.modifyApiKey(memberBanned.getUsername());
        memberBanned.setStatus(MemberStatus.BANNED);
        memberBanned.setDeleteAt(LocalDateTime.now().minusDays(3));
        memberRepository.save(memberBanned);
        reputationRepository.save(new Reputation(memberBanned, 50.0));

        log.info(" 서비스 시연 - 전체 회원 생성 완료: 총 9명 (일반 5명 + 시스템 2명 + 특수 2명)");
    }

    @Transactional
    public void work2() {
        // 초기 데이터도 포트를 통해 접근해 인프라 구현 의존을 도메인 경계 밖으로 제한한다.
        if (categoryPort.count() > 0) return;

        // 디지털/전자
        categoryPort.save(new Category("디지털기기"));
        categoryPort.save(new Category("생활가전"));

        // 가구/생활
        categoryPort.save(new Category("가구/인테리어"));
        categoryPort.save(new Category("생활/주방"));

        // 패션
        categoryPort.save(new Category("여성의류"));
        categoryPort.save(new Category("남성패션/잡화"));

        // 유아동
        categoryPort.save(new Category("유아동"));

        // 취미/레저
        categoryPort.save(new Category("스포츠/레저"));
        categoryPort.save(new Category("도서"));
        categoryPort.save(new Category("게임/취미"));

        // 반려동물/식물
        categoryPort.save(new Category("반려동물용품"));

        // 기타
        categoryPort.save(new Category("기타 중고물품"));

        log.info("테스트 카테고리 생성 완료 - 총 12개");
    }

    @Transactional
    public void work3() {
        if (auctionRepository.count() > 0) return;

        List<Member> members = memberService.findAll();
        List<Category> categories = categoryPort.findAll();

        if (members.size() < 5 || categories.isEmpty()) {
            log.warn("경매 생성 스킵 - 유저 수: {}, 카테고리 수: {}", members.size(), categories.size());
            return;
        }

        //  서비스 시연용: seller 5명 사용
        Member[] sellers = new Member[5];
        for (int i = 0; i < 5; i++) {
            final int index = i + 1;
            sellers[i] = memberService.findByUsername("user" + index)
                    .orElseThrow(() -> new RuntimeException("user" + index + " not found"));
        }

        // 경매 상품 타입 (실제 서비스와 유사하게)
        String[] productTypes = {
                "아이폰", "갤럭시", "노트북", "태블릿", "에어팟", "청소기", "TV", "냉장고",
                "소파", "책상", "의자", "침대", "운동화", "패딩", "가방", "시계",
                "텐트", "자전거", "카메라", "게임기", "도서", "악기", "모니터", "키보드",
                "마우스", "헤드셋", "스피커", "선풍기", "에어컨", "공기청정기"
        };

        int auctionCount = 100;
        log.info(" 서비스 시연 - 경매 생성 시작: {}개 (seller 5명, 카테고리 균등 배분)", auctionCount);

        for (int i = 1; i <= auctionCount; i++) {
            // seller를 순환하여 균등 배정 (5명)
            Member seller = sellers[i % 5];

            // 카테고리를 순환하여 균등 배정 (12개)
            int categoryId = (i % 12) + 1;
            String productType = productTypes[i % productTypes.length];
            Category category = categories.get(categoryId - 1);

            int startPrice = 10000 + (i * 1000);
            int buyNowPrice = startPrice * 2;

            Auction auction = Auction.builder()
                    .seller(seller)
                    .category(category)
                    .name(productType + " #" + i)
                    .description("서비스 시연용 경매 상품입니다. " + i + "번째 상품.")
                    .startPrice(startPrice)
                    .buyNowPrice(buyNowPrice)
                    .startAt(LocalDateTime.now())
                    .endAt(LocalDateTime.now().plusDays(7))
                    .build();

            auctionRepository.save(auction);
        }

        entityManager.flush();
        entityManager.clear();
        log.info("서비스 시연 - 경매 생성 완료: 총 {}개 (seller 5명 균등 배정, 각 카테고리별 8~9개)", auctionCount);
    }

    @Transactional
    public void work4() {
        if (reviewRepository.count() > 0) return;
        Member seller1 = memberService.findByUsername("user1").get();
        Member reviewer1 = memberService.findByUsername("user2").get();
        Member reviewer2 = memberService.findByUsername("user3").get();

        Review review1 = memberService.createReview(5, "친절하시고 배송이 빠릅니다.", seller1, reviewer1.getId());
        reviewRepository.save(review1);
        Review review2 = memberService.createReview(3, "연락이 빠릅니다.", seller1, reviewer2.getId());
        reviewRepository.save(review2);
        Review review3 = memberService.createReview(4, "물건 상태가 좋아요", reviewer1, reviewer2.getId());
        reviewRepository.save(review3);
    }

    @Transactional
    public void work5() {

        if (postRepository.count() > 10) return;

        Member seller1 = memberService.findByUsername("user1").get();
        Category category = categoryPort.findAll().get(0);

        for (int i = 1; i <= 3; i++) {
            Post post = new Post (
                    seller1,
                    "판매 중인 테스트 상품 " + i,
                    "상태가 SALE인 상품입니다.",
                    10000 * i,
                    category,
                    PostStatus.SALE,
                    false
            );
            postRepository.save(post);
        }

        for (int i = 1; i <= 2; i++) {
            Post post = new Post (
                    seller1,
                    "이미 팔린 테스트 상품 " + i,
                    "상태가 SOLD인 상품입니다.",
                    50000 * i,
                    category,
                    PostStatus.SOLD,
                    false
            );
            postRepository.save(post);
        }
    }

}
