package com.back.global.initData;

import com.back.domain.auction.auction.dto.request.AuctionCreateRequest;
import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.auction.auction.service.AuctionService;
import com.back.domain.bid.bid.dto.request.BidCreateRequest;
import com.back.domain.bid.bid.service.BidService;
import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepository;
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

        Member memberSystem = new Member ("system", passwordEncoder.encode("1234"), "시스템", Role.ADMIN,null);
        if (AppConfig.isNotProd()) memberSystem.modifyApiKey(memberSystem.getUsername());
        memberRepository.save(memberSystem);
        Reputation reputation1 = new Reputation(memberSystem, 50.0);
        reputationRepository.save(reputation1);


        Member memberAdmin = new Member("admin", passwordEncoder.encode("1234"), "관리자", Role.ADMIN,null);
        if (AppConfig.isNotProd()) memberAdmin.modifyApiKey(memberAdmin.getUsername());
        memberRepository.save(memberAdmin);
        Reputation reputation2 = new Reputation(memberAdmin, 50.0);
        reputationRepository.save(reputation2);

        Member memberUser1 = new Member("user1", passwordEncoder.encode("1234"), "유저1", Role.USER, null);
        if (AppConfig.isNotProd()) memberUser1.modifyApiKey(memberUser1.getUsername());
        memberRepository.save(memberUser1);
        Reputation reputation3 = new Reputation(memberUser1, 50.0);
        reputationRepository.save(reputation3);

        Member memberUser2 = new Member("user2", passwordEncoder.encode("1234"), "유저2", Role.USER, null);
        if (AppConfig.isNotProd()) memberUser2.modifyApiKey(memberUser2.getUsername());
        memberRepository.save(memberUser2);
        Reputation reputation4 = new Reputation(memberUser2, 50.0);
        reputationRepository.save(reputation4);

        Member memberUser3 = new Member("user3", passwordEncoder.encode("1234"), "유저3", Role.USER, null);
        if (AppConfig.isNotProd()) memberUser3.modifyApiKey(memberUser3.getUsername());
        memberRepository.save(memberUser3);
        Reputation reputation5 = new Reputation(memberUser3, 50.0);
        reputationRepository.save(reputation5);

        // 정지 회원
        Member memberUser4 = new Member("user4", passwordEncoder.encode("1234"), "유저4", Role.USER, null);
        if (AppConfig.isNotProd()) memberUser4.modifyApiKey(memberUser4.getUsername());
        memberUser4.setStatus(MemberStatus.SUSPENDED);
        memberUser4.setSuspendAt(LocalDateTime.now().minusDays(3));
        memberRepository.save(memberUser4);
        Reputation reputation6 = new Reputation(memberUser4, 50.0);
        reputationRepository.save(reputation6);

        // 영구 정지 회원
        Member memberUser5 = new Member("user5", passwordEncoder.encode("1234"), "유저5", Role.USER, null);
        if (AppConfig.isNotProd()) memberUser5.modifyApiKey(memberUser5.getUsername());
        memberUser5.setStatus(MemberStatus.BANNED);
        memberUser5.setDeleteAt(LocalDateTime.now().minusDays(3));
        memberRepository.save(memberUser5);
        Reputation reputation7 = new Reputation(memberUser5, 50.0);
        reputationRepository.save(reputation7);

        log.info("테스트 회원 생성 완료 - 총 8명");
    }

    @Transactional
    public void work2() {
        if (categoryRepository.count() > 0) return;

        // 디지털/전자
        categoryRepository.save(new Category("디지털기기"));
        categoryRepository.save(new Category("생활가전"));

        // 가구/생활
        categoryRepository.save(new Category("가구/인테리어"));
        categoryRepository.save(new Category("생활/주방"));

        // 패션
        categoryRepository.save(new Category("여성의류"));
        categoryRepository.save(new Category("남성패션/잡화"));

        // 유아동
        categoryRepository.save(new Category("유아동"));

        // 취미/레저
        categoryRepository.save(new Category("스포츠/레저"));
        categoryRepository.save(new Category("도서"));
        categoryRepository.save(new Category("게임/취미"));

        // 반려동물/식물
        categoryRepository.save(new Category("반려동물용품"));

        // 기타
        categoryRepository.save(new Category("기타 중고물품"));

        log.info("테스트 카테고리 생성 완료 - 총 12개");
    }

    @Transactional
    public void work3() {
        if (auctionRepository.count() > 0) return;

        List<Member> members = memberService.findAll();
        List<Category> categories = categoryRepository.findAll();

        if (members.size() < 3 || categories.isEmpty()) {
            return;
        }

        Member seller1 = memberService.findByUsername("user1").get();
        Member seller2 = memberService.findByUsername("user2").get();
        Member seller3 = memberService.findByUsername("user3").get();

        // 경매 상품 데이터 (20개)
        String[][] auctionData = {
                // {상품명, 설명, 시작가, 즉시구매가, 카테고리ID}
                {"아이폰 14 Pro", "1년 사용, 배터리 효율 95%", "800000", "1200000", "1"},
                {"갤럭시 탭 S8", "거의 새 제품, 케이스 포함", "500000", "750000", "1"},
                {"에어팟 프로 2세대", "미개봉 새 제품", "250000", "320000", "1"},
                {"LG 그램 노트북", "2023년형, 17인치", "1200000", "1600000", "1"},
                {"애플워치 SE", "실버, 40mm", "200000", "280000", "1"},

                {"다이슨 무선청소기", "V11, 2022년 구매", "350000", "500000", "2"},
                {"공기청정기", "삼성 블루스카이", "180000", "250000", "2"},
                {"LG 스타일러", "2021년형, 정상 작동", "800000", "1100000", "2"},

                {"이케아 소파", "3인용, 블루 컬러", "150000", "230000", "3"},
                {"서재 책상 세트", "원목 책상 + 의자", "200000", "300000", "3"},

                {"나이키 에어맥스", "270 사이즈, 새 제품", "120000", "160000", "6"},
                {"아디다스 패딩", "M 사이즈, 블랙", "80000", "120000", "6"},

                {"유아 카시트", "안전 인증, 2년 사용", "100000", "150000", "7"},

                {"캠핑 텐트", "4인용, 몇 번 사용", "180000", "250000", "8"},
                {"로드 자전거", "자이언트 브랜드", "400000", "600000", "8"},

                {"해리포터 전집", "영문판, 하드커버", "50000", "80000", "9"},
                {"경제학 원론 교재", "맨큐의 경제학 10판", "30000", "45000", "9"},

                {"PS5 디지털 에디션", "1년 사용, 컨트롤러 2개", "350000", "480000", "10"},

                {"강아지 하우스", "중형견용, 새 제품", "80000", "120000", "11"},

                {"빈티지 카메라", "필름 카메라, 수집용", "150000", "220000", "12"}
        };

        // 판매자 순환 (user1, user2, user3)
        Member[] sellers = {seller1, seller2, seller3};

        // 20개 경매 생성
        for (int i = 0; i < auctionData.length; i++) {
            String[] data = auctionData[i];
            Member seller = sellers[i % 3]; // 순환 배정

            auctionService.createAuction(
                    new AuctionCreateRequest(
                            data[0],  // 상품명
                            data[1],  // 설명
                            Integer.parseInt(data[2]),  // 시작가
                            Integer.parseInt(data[3]),  // 즉시구매가
                            Integer.parseInt(data[4]),  // 카테고리 ID
                            168,  // 7일 후 종료 (168시간)
                            null  // 이미지 없음
                    ),
                    seller.getId()
            );
        }

        // 일부 경매에 입찰 추가 (캐시 테스트용)
        // 경매 판매자 정보:
        // - 경매 1번: user1 (sellers[0])
        // - 경매 2번: user2 (sellers[1])
        // - 경매 3번: user3 (sellers[2])
        // - 경매 4번: user1 (sellers[0])
        // - 경매 5번: user2 (sellers[1])
        // - 경매 6번: user3 (sellers[2])

        Member bidder1 = memberService.findByUsername("user2").get();  // user2
        Member bidder2 = memberService.findByUsername("user3").get();  // user3

        // 경매 1번(판매자: user1)에 user2가 입찰
        bidService.createBid(1, new BidCreateRequest(850000), bidder1.getId());

        // 경매 4번(판매자: user1)에 user3이 입찰
        bidService.createBid(4, new BidCreateRequest(1300000), bidder2.getId());
        // 경매 4번에 user2가 재입찰
        bidService.createBid(4, new BidCreateRequest(1350000), bidder1.getId());

        // 경매 3번(판매자: user3)에 user2가 입찰
        bidService.createBid(3, new BidCreateRequest(270000), bidder1.getId());

        log.info("테스트 경매 생성 완료 - 총 {}개", auctionData.length);
        log.info("테스트 입찰 생성 완료 - 총 4건");
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
        Category category = categoryRepository.findAll().get(0);

        for (int i = 1; i <= 3; i++) {
            Post post = Post.builder()
                    .seller(seller1)
                    .title("판매 중인 테스트 상품 " + i)
                    .content("상태가 SALE인 상품입니다.")
                    .price(10000 * i)
                    .category(category)
                    .status(PostStatus.SALE) //
                    .build();
            postRepository.save(post);
        }

        for (int i = 1; i <= 2; i++) {
            Post post = Post.builder()
                    .seller(seller1)
                    .title("이미 팔린 테스트 상품 " + i)
                    .content("상태가 SOLD인 상품입니다.")
                    .price(50000 * i)
                    .category(category)
                    .status(PostStatus.SOLD) //
                    .build();
            postRepository.save(post);
        }
    }

}
