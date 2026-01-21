package com.back.global.initData;

import com.back.domain.auction.auction.dto.request.AuctionCreateRequest;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.auction.auction.service.AuctionService;
import com.back.domain.bid.bid.dto.request.BidCreateRequest;
import com.back.domain.bid.bid.service.BidService;
import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.app.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {
    @Autowired
    @Lazy
    private BaseInitData self;
    private final MemberService memberService;
    private final CategoryRepository categoryRepository;
    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;
    private final BidService bidService;

    @Bean
    ApplicationRunner baseInitDataApplicationRunner() {
        return args -> {
            self.work1();
            self.work2();
            self.work3();
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

        Member seller1 = memberService.findByUsername("user1").get();
        auctionService.createAuction(new AuctionCreateRequest("시집", "이성복 시집 팝니다.", 5000, 8000, 3, 168, null), seller1.getId());

        Member bidder1 = memberService.findByUsername("user2").get();
        bidService.createBid(1, new BidCreateRequest(6000), bidder1.getId());


        Member seller2 = memberService.findByUsername("user2").get();
        auctionService.createAuction(new AuctionCreateRequest("아이폰13", "거의 새거입니다 ㅎ.", 150000, 180000, 1, 168, null), seller2.getId());
    }
}
