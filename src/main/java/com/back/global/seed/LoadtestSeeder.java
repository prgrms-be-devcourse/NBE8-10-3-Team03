package com.back.global.seed;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.auction.auction.service.AuctionService;
import com.back.domain.bid.bid.service.BidService;
import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.image.image.entity.Image;
import com.back.domain.image.image.repository.ImageRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.MemberStatus;
import com.back.domain.member.member.enums.Role;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.member.reputation.entity.Reputation;
import com.back.domain.member.reputation.repository.ReputationRepository;
import com.back.domain.member.review.repository.ReviewRepository;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.entity.PostImage;
import com.back.domain.post.post.entity.PostStatus;
import com.back.domain.post.post.repository.PostRepository;
import com.back.global.app.AppConfig;
import com.back.global.initData.BaseInitData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Profile("loadtest")
@RequiredArgsConstructor
@Component
public class LoadtestSeeder{

    @Autowired
    @Lazy
    private LoadtestSeeder self;
    private final MemberService memberService;
    private final CategoryRepository categoryRepository;
    private final AuctionRepository auctionRepository;
    private final ReputationRepository reputationRepository;
    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final AuctionService auctionService;
    private final BidService bidService;
    private final PostRepository postRepository;
    private final ImageRepository imageRepository;

    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Bean
    @Profile("loadtest")
    ApplicationRunner loadTestSeederApplicationRunner() {
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

        for (int i = 1; i <= 1000; i++) {
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
    }

    @Transactional
    public void work3() {
        if (auctionRepository.count() > 0) return;

        List<Member> members = memberService.findAll();
        List<Category> categories = categoryRepository.findAll();

        if (members.size() < 5 || categories.isEmpty()) {
            return;
        }

        //  서비스 시연용: seller 5명 사용
        Member[] sellers = new Member[1000];
        for (int i = 0; i < 1000; i++) {
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

        int auctionCount = 100_000;

        for (int i = 1; i <= auctionCount; i++) {
            // seller를 순환하여 균등 배정 (5명)
            Member seller = sellers[i % 1000];

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

            if ( i % 1000 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.flush();
        entityManager.clear();
    }

    @Transactional
    public void work4() {
        final int targetPostCount = 10_000;
        if (postRepository.count() >= targetPostCount) return;

        List<Member> sellers = memberService.findAll().stream()
                .filter(member -> member.getStatus() == MemberStatus.ACTIVE)
                .toList();
        List<Category> categories = categoryRepository.findAll();

        if (sellers.size() < 1_000 || categories.isEmpty()) return;

        // 고정 분포: SALE 70% / RESERVED 20% / SOLD 10%
        final int saleCount = 7_000;
        final int reservedCount = 2_000;

        // 고정 분포: 0장 60% / 1장 30% / 3장 10%
        final int zeroImageCount = 6_000;
        final int oneImageCount = 3_000;
        final String[] sampleImageUrls = {
                "/uploads/loadtest/sample-1.jpg",
                "/uploads/loadtest/sample-2.jpg",
                "/uploads/loadtest/sample-3.jpg",
                "/uploads/loadtest/sample-4.jpg",
                "/uploads/loadtest/sample-5.jpg"
        };

        List<Integer> hotspotIds = new ArrayList<>();

        for (int i = 1; i <= targetPostCount; i++) {
            Member seller = sellers.get((i - 1) % 1_000);
            Category category = categories.get((i - 1) % categories.size());

            PostStatus status;
            if (i <= saleCount) status = PostStatus.SALE;
            else if (i <= saleCount + reservedCount) status = PostStatus.RESERVED;
            else status = PostStatus.SOLD;

            Post post = new Post(
                    seller,
                    "[LT-POST] 상품 " + i,
                    "[LT-POST] loadtest seed content #" + i,
                    10_000 + (i * 10),
                    category,
                    status,
                    false
            );

            if (i > zeroImageCount) {
                int imageCount = (i <= zeroImageCount + oneImageCount) ? 1 : 3;
                for (int imgIdx = 1; imgIdx <= imageCount; imgIdx++) {
                    int sampleIndex = Math.floorMod(i + imgIdx - 2, sampleImageUrls.length);
                    Image image = imageRepository.save(new Image(
                            sampleImageUrls[sampleIndex]
                    ));
                    post.addPostImage(new PostImage(post, image));
                }
            }

            postRepository.save(post);

            if (hotspotIds.size() < 3) {
                hotspotIds.add(post.getId());
            }

            if (i % 1000 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.flush();
        entityManager.clear();

        System.out.println("[LOADTEST] post seed completed: total=10000, status=7000/2000/1000, images=6000/3000/1000");
        System.out.println("[LOADTEST] hotspot post ids (for focused detail mode): " + hotspotIds);
        System.out.println("[LOADTEST] export as env: POST_HOT_IDS=" + hotspotIds.get(0) + "," + hotspotIds.get(1) + "," + hotspotIds.get(2));
    }

}
