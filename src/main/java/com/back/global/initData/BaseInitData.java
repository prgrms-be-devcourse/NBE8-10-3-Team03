package com.back.global.initData;

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

    @Bean
    ApplicationRunner baseInitDataApplicationRunner() {
        return args -> {
            self.work1();
            self.work2();
        };
    }

    @Transactional
    public void work1() {
        if (memberService.count() > 0) return;

        Member memberSystem = memberService.join("system", "1234", "시스템");
        if (AppConfig.isNotProd()) memberSystem.modifyApiKey(memberSystem.getUsername());

        Member memberAdmin = memberService.join("admin", "1234", "관리자");
        if (AppConfig.isNotProd()) memberAdmin.modifyApiKey(memberAdmin.getUsername());

        Member memberUser1 = memberService.join("user1", "1234", "유저1");
        if (AppConfig.isNotProd()) memberUser1.modifyApiKey(memberUser1.getUsername());

        Member memberUser2 = memberService.join("user2", "1234", "유저2");
        if (AppConfig.isNotProd()) memberUser2.modifyApiKey(memberUser2.getUsername());

        Member memberUser3 = memberService.join("user3", "1234", "유저3");
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
}
