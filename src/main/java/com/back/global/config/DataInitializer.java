package com.back.global.config;

import com.back.domain.category.entity.Category;
import com.back.domain.category.repository.CategoryRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    @Bean
    @Profile({"dev", "test"})
    public CommandLineRunner initData(
            MemberRepository memberRepository,
            CategoryRepository categoryRepository
    ) {
        return args -> {
            // 테스트용 회원 생성
            if (memberRepository.count() == 0) {
                Member testMember = new Member();
                memberRepository.save(testMember);
                System.out.println("테스트 회원 생성 완료 (ID: " + testMember.getId() + ")");
            }

            // 테스트용 카테고리 생성
            if (categoryRepository.count() == 0) {
                categoryRepository.save(new Category("전자기기"));
                categoryRepository.save(new Category("의류"));
                categoryRepository.save(new Category("도서"));
                categoryRepository.save(new Category("가구"));
                categoryRepository.save(new Category("기타"));
                System.out.println("테스트 카테고리 생성 완료");
            }
        };
    }
}

