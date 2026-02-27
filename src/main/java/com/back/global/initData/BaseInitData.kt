package com.back.global.initData

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.service.port.CategoryPort
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.enums.MemberStatus
import com.back.domain.member.member.enums.Role
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.member.service.MemberService
import com.back.domain.member.reputation.entity.Reputation
import com.back.domain.member.reputation.repository.ReputationRepository
import com.back.domain.member.review.repository.ReviewRepository
import com.back.domain.post.post.entity.Post
import com.back.domain.post.post.entity.PostStatus
import com.back.domain.post.post.repository.PostRepository
import com.back.global.app.AppConfig
import com.back.global.exception.ServiceException
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

@Profile("!loadtest")
@Configuration
class BaseInitData(
    private val memberService: MemberService,
    private val categoryPort: CategoryPort,
    private val auctionRepository: AuctionRepository,
    private val reputationRepository: ReputationRepository,
    private val reviewRepository: ReviewRepository,
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val passwordEncoder: PasswordEncoder,
    private val entityManager: EntityManager
) {
    @Autowired
    @Lazy
    private lateinit var self: BaseInitData

    @Bean
    @ConditionalOnProperty(name = ["initdata.enabled"], havingValue = "true")
    fun baseInitDataApplicationRunner(): ApplicationRunner = ApplicationRunner {
        self.seedMembers()
        self.seedCategories()
        self.seedAuctions()
        self.seedReviews()
        self.seedPosts()
    }

    @Transactional
    fun seedMembers() {
        if (memberService.count() > 0) return

        val adminMembers = listOf(
            Member("system", passwordEncoder.encode("1234"), "시스템", Role.ADMIN, null),
            Member("admin", passwordEncoder.encode("1234"), "관리자", Role.ADMIN, null)
        )

        adminMembers.forEach { member ->
            if (AppConfig.isNotProd()) member.modifyApiKey(member.username)
            memberRepository.save(member)
            reputationRepository.save(Reputation(member, 50.0))
        }

        log.info("서비스 시연 - 일반 유저 생성 시작: 5명")

        (1..5).forEach { index ->
            val member = Member(
                "user$index",
                passwordEncoder.encode("1234"),
                "유저$index",
                Role.USER,
                null
            )
            if (AppConfig.isNotProd()) member.modifyApiKey(member.username)

            memberRepository.save(member)
            reputationRepository.save(Reputation(member, Random.nextDouble(40.0, 100.0)))
        }

        val suspended = Member("user6", passwordEncoder.encode("1234"), "정지유저", Role.USER, null).apply {
            if (AppConfig.isNotProd()) modifyApiKey(username)
            status = MemberStatus.SUSPENDED
            suspendAt = LocalDateTime.now().minusDays(3)
        }
        memberRepository.save(suspended)
        reputationRepository.save(Reputation(suspended, 50.0))

        val banned = Member("user7", passwordEncoder.encode("1234"), "영구정지유저", Role.USER, null).apply {
            if (AppConfig.isNotProd()) modifyApiKey(username)
            status = MemberStatus.BANNED
            deleteAt = LocalDateTime.now().minusDays(3)
        }
        memberRepository.save(banned)
        reputationRepository.save(Reputation(banned, 50.0))

        entityManager.flush()
        entityManager.clear()
        log.info("서비스 시연 - 전체 회원 생성 완료: 총 9명 (일반 5명 + 시스템 2명 + 특수 2명)")
    }

    @Transactional
    fun seedCategories() {
        if (categoryPort.count() > 0) return

        listOf(
            "디지털기기",
            "생활가전",
            "가구/인테리어",
            "생활/주방",
            "여성의류",
            "남성패션/잡화",
            "유아동",
            "스포츠/레저",
            "도서",
            "게임/취미",
            "반려동물용품",
            "기타 중고물품"
        ).forEach { categoryName ->
            categoryPort.save(Category(categoryName))
        }

        log.info("테스트 카테고리 생성 완료 - 총 12개")
    }

    @Transactional
    fun seedAuctions() {
        if (auctionRepository.count() > 0) return

        val categories = categoryPort.findAll()
        if (categories.isEmpty()) {
            log.warn("경매 생성 스킵 - 카테고리 없음")
            return
        }

        val sellers = (1..5).map { index ->
            memberService.findByUsername("user$index")
                ?: throw  IllegalStateException("user$index not found")
        }

        val productTypes = listOf(
            "아이폰", "갤럭시", "노트북", "태블릿", "에어팟", "청소기", "TV", "냉장고",
            "소파", "책상", "의자", "침대", "운동화", "패딩", "가방", "시계",
            "텐트", "자전거", "카메라", "게임기", "도서", "악기", "모니터", "키보드",
            "마우스", "헤드셋", "스피커", "선풍기", "에어컨", "공기청정기"
        )

        val auctionCount = 100
        log.info("서비스 시연 - 경매 생성 시작: {}개", auctionCount)

        (1..auctionCount).forEach { index ->
            val seller = sellers[index % sellers.size]
            val category = categories[index % categories.size]
            val startPrice = 10_000 + (index * 1_000)
            val buyNowPrice = startPrice * 2

            val auction = Auction.builder()
                .seller(seller)
                .category(category)
                .name("${productTypes[index % productTypes.size]} #$index")
                .description("서비스 시연용 경매 상품입니다. ${index}번째 상품.")
                .startPrice(startPrice)
                .buyNowPrice(buyNowPrice)
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(7))
                .build()

            auctionRepository.save(auction)
        }

        entityManager.flush()
        entityManager.clear()
        log.info("서비스 시연 - 경매 생성 완료: 총 {}개", auctionCount)
    }

    @Transactional
    fun seedReviews() {
        if (reviewRepository.count() > 0) return

        val seller = memberService.findByUsername("user1")
            ?: throw ServiceException("404-1", "초기 데이터 생성 실패: user를 찾을 수 없습니다.")
        val reviewer1 = memberService.findByUsername("user2")
            ?: throw ServiceException("404-1", "초기 데이터 생성 실패: user를 찾을 수 없습니다.")
        val reviewer2 = memberService.findByUsername("user3")
            ?: throw ServiceException("404-1", "초기 데이터 생성 실패: user를 찾을 수 없습니다.")

        reviewRepository.save(memberService.createReview(5, "친절하시고 배송이 빠릅니다.", seller, reviewer1.id))
        reviewRepository.save(memberService.createReview(3, "연락이 빠릅니다.", seller, reviewer2.id))
        reviewRepository.save(memberService.createReview(4, "물건 상태가 좋아요", reviewer1, reviewer2.id))
    }

    @Transactional
    fun seedPosts() {
        if (postRepository.count() > 10) return

        val seller = memberService.findByUsername("user1")
            ?: throw ServiceException("404-1", "초기 데이터 생성 실패: user를 찾을 수 없습니다.")
        val category = categoryPort.findAll().firstOrNull()
            ?: throw IllegalStateException("카테고리가 존재하지 않습니다.")

        (1..3).forEach { index ->
            postRepository.save(
                Post(
                    seller = seller,
                    title = "판매 중인 테스트 상품 $index",
                    content = "상태가 SALE인 상품입니다.",
                    price = 10_000 * index,
                    category = category,
                    status = PostStatus.SALE,
                    deleted = false
                )
            )
        }

        (1..2).forEach { index ->
            postRepository.save(
                Post(
                    seller = seller,
                    title = "이미 팔린 테스트 상품 $index",
                    content = "상태가 SOLD인 상품입니다.",
                    price = 50_000 * index,
                    category = category,
                    status = PostStatus.SOLD,
                    deleted = false
                )
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BaseInitData::class.java)
    }
}
