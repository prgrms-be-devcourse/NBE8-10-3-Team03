package com.back.domain.search.search.service;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
import com.back.domain.search.search.dto.UnifiedSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {
    private final PostRepository postRepository;
    private final AuctionRepository auctionRepository;

    // 통합 검색 (POST + AUCTION)
    public Page<UnifiedSearchResponse> searchUnified(String keyword, Pageable pageable) {
        // 1. Post 검색
        Page<Post> posts = postRepository.search(keyword, pageable);

        // 2. Auction 검색
        Page<Auction> auctions = auctionRepository.search(keyword, pageable);

        // 3. 결과를 UnifiedSearchResponse로 변환
        List<UnifiedSearchResponse> combinedResults = new ArrayList<>();

        // Post 변환
        posts.forEach(post -> combinedResults.add(
            UnifiedSearchResponse.builder()
                .id(post.getId())
                .type("POST")
                .title(post.getTitle())
                .price(post.getPrice())
                .status(post.getStatus().name())
                .categoryName(post.getCategory().getName())
                .thumbnailUrl(post.getPostImages().isEmpty() ? null
                    : post.getPostImages().get(0).getImage().getUrl())
                .createDate(post.getCreateDate())
                .build()
        ));

        // Auction 변환
        auctions.forEach(auction -> combinedResults.add(
            UnifiedSearchResponse.builder()
                .id(auction.getId())
                .type("AUCTION")
                .title(auction.getName())
                .price(auction.getStartPrice())
                .status(auction.getStatus().name())
                .categoryName(auction.getCategory().getName())
                .thumbnailUrl(auction.getAuctionImages().isEmpty() ? null
                    : auction.getAuctionImages().get(0).getImage().getUrl())
                .createDate(auction.getCreateDate())
                .build()
        ));

        // 4. 최신순 정렬
        List<UnifiedSearchResponse> sortedResults = combinedResults.stream()
            .sorted(Comparator.comparing(UnifiedSearchResponse::getCreateDate).reversed())
            .collect(Collectors.toList());

        // 5. 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedResults.size());

        List<UnifiedSearchResponse> pagedResults = start >= sortedResults.size()
            ? new ArrayList<>()
            : sortedResults.subList(start, end);

        return new PageImpl<>(pagedResults, pageable, sortedResults.size());
    }
}