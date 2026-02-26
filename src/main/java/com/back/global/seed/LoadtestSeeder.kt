package com.back.global.seed

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.repository.CategoryRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.enums.Role
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.member.service.MemberService
import com.back.domain.member.reputation.entity.Reputation
import com.back.domain.member.reputation.repository.ReputationRepository
import com.back.global.app.AppConfig
import jakarta.persistence.EntityManager
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

@Profile("loadtest")
@Configuration
class LoadtestSeeder(
    @Lazy private val self: LoadtestSeeder, // 내부 호출용 self 주입
    private val memberService: MemberService,
    private val categoryRepository: CategoryRepository,
    private val auctionRepository: AuctionRepository,
    private val reputationRepository: ReputationRepository,
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val entityManager: EntityManager
) {

    @Bean
    @Profile("loadtest")
    fun loadTestSeederApplicationRunner() = ApplicationRunner {
        self.work1()
        self.work2()
        self.work3()
    }

    @Transactional
    fun work1() {
        if (memberService.count() > 0) return

        repeat(1000) { i ->
            val index = i + 1
            val member = Member("user$index", passwordEncoder.encode("1234"), "유저$index", Role.USER, null).apply {
                if (AppConfig.isNotProd()) modifyApiKey(username)
            }
            memberRepository.save(member)
            reputationRepository.save(Reputation(member, Random.nextDouble(40.0, 100.0)))
        }
        entityManager.flush()
        entityManager.clear()
    }

    @Transactional
    fun work2() {
        if (categoryRepository.count() > 0) return

        val categories = listOf(
            "디지털기기", "생활가전", "가구/인테리어", "생활/주방",
            "여성의류", "남성패션/잡화", "유아동", "스포츠/레저",
            "도서", "게임/취미", "반려동물용품", "기타 중고물품"
        )
        categories.forEach { categoryRepository.save(Category(it)) }
    }

    @Transactional
    fun work3() {
        if (auctionRepository.count() > 0) return

        val members = memberService.findAll()
        val categories = categoryRepository.findAll()
        if (members.size < 1000 || categories.isEmpty()) return

        val productTypes = arrayOf(
            "아이폰", "갤럭시", "노트북", "태블릿", "에어팟", "청소기", "TV", "냉장고",
            "소파", "책상", "의자", "침대", "운동화", "패딩", "가방", "시계",
            "텐트", "자전거", "카메라", "게임기", "도서", "악기", "모니터", "키보드",
            "마우스", "헤드셋", "스피커", "선풍기", "에어컨", "공기청정기"
        )

        repeat(100_000) { i ->
            val index = i + 1
            val seller = members[i % 1000]
            val category = categories[i % categories.size]
            val productName = productTypes[i % productTypes.size]

            val startPrice = 10000 + (index * 100)
            auctionRepository.save(
                Auction.builder()
                    .seller(seller)
                    .category(category)
                    .name("$productName #$index")
                    .description("서비스 시연용 경매 상품입니다. $index 번째 상품.")
                    .startPrice(startPrice)
                    .buyNowPrice(startPrice * 2)
                    .startAt(LocalDateTime.now())
                    .endAt(LocalDateTime.now().plusDays(7))
                    .build()
            )

            if (index % 1000 == 0) { // 대량 데이터 flush 최적화
                entityManager.flush()
                entityManager.clear()
            }
        }
        entityManager.flush()
        entityManager.clear()
    }
}