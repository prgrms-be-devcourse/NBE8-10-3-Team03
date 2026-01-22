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
import com.back.domain.member.member.service.MemberService;
import com.back.domain.member.review.entity.Review;
import com.back.domain.member.review.repository.ReviewRepository;
import com.back.global.app.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {
    @Autowired
    @Lazy
    private BaseInitData self;
    private final MemberService memberService;
    private final CategoryRepository categoryRepository;
    private final AuctionRepository auctionRepository;
    private final ReviewRepository reviewRepository;
    private final AuctionService auctionService;
    private final BidService bidService;

    @Bean
    ApplicationRunner baseInitDataApplicationRunner() {
        return args -> {
            self.work1();
            self.work2();
            self.work3();
            self.work4();
        };
    }

    @Transactional
    public void work1() {
        if (memberService.count() > 0) return;

        Member memberSystem = memberService.join("system", "1234", "시스템",  null);
        if (AppConfig.isNotProd()) memberSystem.modifyApiKey(memberSystem.getUsername());

        Member memberAdmin = memberService.join("admin", "1234", "관리자", null);
        if (AppConfig.isNotProd()) memberAdmin.modifyApiKey(memberAdmin.getUsername());

        Member memberUser1 = memberService.join("user1", "1234", "유저1", null);
        if (AppConfig.isNotProd()) memberUser1.modifyApiKey(memberUser1.getUsername());

        Member memberUser2 = memberService.join("user2", "1234", "유저2", null);
        if (AppConfig.isNotProd()) memberUser2.modifyApiKey(memberUser2.getUsername());

        Member memberUser3 = memberService.join("user3", "1234", "유저3", null);
        if (AppConfig.isNotProd()) memberUser3.modifyApiKey(memberUser3.getUsername());

        Member memberUser4 = memberService.join("user4", "1234", "유저4", null);
        if (AppConfig.isNotProd()) memberUser3.modifyApiKey(memberUser4.getUsername());
        memberUser4.setActive(false);
        memberUser4.setSuspendAt(LocalDateTime.now().minusDays(3));
        System.out.println("테스트 회원 생성 완료");
    }

    @Transactional
    public void work2() {
        if (categoryRepository.count() > 0) return;

        categoryRepository.save(new Category("전자기기"));
        categoryRepository.save(new Category("의류"));
        categoryRepository.save(new Category("도서"));
        categoryRepository.save(new Category("가구"));
        categoryRepository.save(new Category("기타"));

        System.out.println("테스트 카테고리 생성 완료");
    }

    @Transactional
    public void work3() {
        if (auctionRepository.count() > 0) return;

        List<Member> members = memberService.findAll();
        List<Category> categories = categoryRepository.findAll();

        if (members.size() < 3 || categories.isEmpty()) {
            return;
        }

        // 경매 1: Service를 통한 경매 생성 (시집)
        Member seller1 = memberService.findByUsername("user1").get();
        auctionService.createAuction(
                new AuctionCreateRequest("시집", "이성복 시집 팝니다.", 5000, 8000, 3, 168, null),
                seller1.getId()
        );

        // 경매 1에 입찰
        Member bidder1 = memberService.findByUsername("user2").get();
        bidService.createBid(1, new BidCreateRequest(6000), bidder1.getId());

        // 경매 2: Service를 통한 경매 생성 (아이폰)
        Member seller2 = memberService.findByUsername("user2").get();
        auctionService.createAuction(
                new AuctionCreateRequest("아이폰13", "거의 새거입니다 ㅎ.", 150000, 180000, 1, 168, null),
                seller2.getId()
        );

        // 경매 3: 엔티티 직접 생성 (낙찰 테스트용 - 1분 후 종료)
        Category category = categories.get(0); // 전자기기
        Auction auction3 = Auction.builder()
                .seller(seller1)
                .category(category)
                .name("테스트 경매 (1분 후 종료)")
                .description("낙찰 기능 테스트용 경매입니다.")
                .startPrice(10000)
                .buyNowPrice(50000)
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusMinutes(1))
                .build();
        auctionRepository.save(auction3);

        System.out.println("테스트 경매 생성 완료");
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
}
