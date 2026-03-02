package com.back.domain.search.search.service

import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.post.post.dto.PostListResponse
import com.back.domain.post.post.repository.PostRepository
import com.back.domain.search.search.dto.SearchCursor
import com.back.domain.search.search.dto.SearchListResponse
import com.back.domain.search.search.dto.SearchResponse
import com.back.domain.search.search.dto.UnifiedSearchListItem
import com.back.domain.search.search.dto.UnifiedSearchResponse
import com.back.domain.search.search.service.port.SearchProvider
import com.back.domain.search.search.service.port.UnifiedSearchRepository
import com.back.domain.search.search.service.projection.CursorCodec
import com.back.domain.search.search.service.projection.UnifiedSearchRow
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class SearchService(

    private val postRepository: PostRepository,
    private val auctionRepository: AuctionRepository,


    //private val searchProviders: List<SearchProvider>
    private val unifiedSearchRepository: UnifiedSearchRepository
) {

    // 통합 검색 (POST + AUCTION)
//    fun searchUnified(keyword: String, sort: String, pageable: Pageable): Slice<UnifiedSearchResponse> {
//        val kw = toBooleanKw(keyword)
//        val p = PageRequest.of(pageable.pageNumber, pageable.pageSize) // Sort 제거
//
//        val rows = when (sort.lowercase()) {
//            "newest" -> unifiedSearchRepository.searchUnifiedNewest(kw, p)
//            "oldest" -> unifiedSearchRepository.searchUnifiedOldest(kw, p)
//            else -> unifiedSearchRepository.searchUnifiedRelevance(kw, p) // default relevance
//        }
//
//        return rows.map { it.toDto() }
//    }

//    fun searchUnifiedCursor(keyword: String, sort: String, size: Int, cursor: String?): SearchResponse {
//        val kw = toBooleanKw(keyword)
//        val pageable = PageRequest.of(0, size.coerceIn(1, 50))
//
//        val cur = cursor?.let { CursorCodec.decode(it) }
//
//        val slice = when (sort.lowercase()) {
//            "newest" -> unifiedSearchRepository.searchNewestCursor(
//                kw,
//                cur?.createDate, cur?.typeRank, cur?.id,
//                pageable
//            )
//            "oldest" -> unifiedSearchRepository.searchOldestCursor(
//                kw,
//                cur?.createDate, cur?.typeRank, cur?.id,
//                pageable
//            )
//            else -> unifiedSearchRepository.searchRelevanceCursor(
//                kw,
//                cur?.score, cur?.createDate, cur?.typeRank, cur?.id,
//                pageable
//            )
//        }
//
//        val content = slice.content.map { it.toDto() } // score/typeRank 버림
//
//        val nextCursor = slice.content.lastOrNull()?.let { last ->
//            val nc = SearchCursor(
//                sort = sort.lowercase(),
//                score = last.score,               // newest/oldest는 null
//                createDate = last.createDate,
//                typeRank = last.typeRank,
//                id = last.id
//            )
//            CursorCodec.encode(nc)
//        }
//
//        return SearchResponse.ofCursor(
//            content = content,
//            size = pageable.pageSize,
//            hasNext = slice.hasNext(),
//            nextCursor = nextCursor
//        )
//    }
//
//    private fun normalizeSort(sort: String): String =
//        when (sort.lowercase()) {
//            "relevance", "newest", "oldest" -> sort.lowercase()
//            else -> "relevance"
//        }
//
//    fun toBooleanKw(input: String): String =
//        input.trim()
//            .split(Regex("\\s+"))
//            .filter { it.isNotBlank() }
//            .joinToString(" ") { "+${it}*" }
//
//    private fun UnifiedSearchRow.toDto() = UnifiedSearchResponse(
//        id = id,
//        type = type,
//        title = title,
//        price = price,
//        status = status,
//        statusDisplayName = statusDisplayName,
//        categoryName = categoryName,
//        thumbnailUrl = thumbnailUrl,
//        createDate = createDate
//    )
fun toBooleanKw(input: String): String =
        input.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { "+${it}*" }

    fun searchUnifiedListCursor(
        keyword: String,
        sort: String,
        size: Int,
        cursor: String?
    ): SearchListResponse {

        val safeSize = size.coerceIn(1, 50)
        val pageable = PageRequest.of(0, safeSize)
        val kw = toBooleanKw(keyword)

        val cur = cursor?.let { CursorCodec.decode(it) }
        val cursorCreateDate: LocalDateTime? = cur?.createDate?.let { LocalDateTime.parse(it) }

        return when (sort.lowercase()) {
            "newest" -> newest(kw, pageable, cur, cursorCreateDate)
            "oldest" -> oldest(kw, pageable, cur, cursorCreateDate)
            else -> relevance(kw, pageable, cur, cursorCreateDate) // default relevance
        }
    }

    private fun newest(
        kw: String,
        pageable: PageRequest,
        cur: SearchCursor?,
        cursorCreateDate: LocalDateTime?
    ): SearchListResponse {

        val hitSlice = unifiedSearchRepository.searchNewestHits(
            kw = kw,
            cursorCreateDate = cursorCreateDate,
            cursorTypeRank = cur?.typeRank,
            cursorId = cur?.id,
            pageable = pageable
        )

        val hits = hitSlice.content
        val items = loadItemsByHits(hits)

        val nextCursor = hits.lastOrNull()?.let { last ->
            CursorCodec.encode(
                SearchCursor(
                    sort = "newest",
                    createDate = last.createDate.toString(), // ✅ String
                    typeRank = last.typeRank,
                    id = last.id
                )
            )
        }

        return SearchListResponse(
            content = items,
            size = pageable.pageSize,
            hasNext = hitSlice.hasNext(),
            nextCursor = if (hitSlice.hasNext()) nextCursor else null
        )
    }

    private fun oldest(
        kw: String,
        pageable: PageRequest,
        cur: SearchCursor?,
        cursorCreateDate: LocalDateTime?
    ): SearchListResponse {

        val hitSlice = unifiedSearchRepository.searchOldestHits(
            kw = kw,
            cursorCreateDate = cursorCreateDate,
            cursorTypeRank = cur?.typeRank,
            cursorId = cur?.id,
            pageable = pageable
        )

        val hits = hitSlice.content
        val items = loadItemsByHits(hits)

        val nextCursor = hits.lastOrNull()?.let { last ->
            CursorCodec.encode(
                SearchCursor(
                    sort = "oldest",
                    createDate = last.createDate.toString(), // ✅ String
                    typeRank = last.typeRank,
                    id = last.id
                )
            )
        }

        return SearchListResponse(
            content = items,
            size = pageable.pageSize,
            hasNext = hitSlice.hasNext(),
            nextCursor = if (hitSlice.hasNext()) nextCursor else null
        )
    }

    private fun relevance(
        kw: String,
        pageable: PageRequest,
        cur: SearchCursor?,
        cursorCreateDate: LocalDateTime?
    ): SearchListResponse {

        val hitSlice = unifiedSearchRepository.searchRelevanceHits(
            kw = kw,
            cursorScore = cur?.score,
            cursorCreateDate = cursorCreateDate,
            cursorTypeRank = cur?.typeRank,
            cursorId = cur?.id,
            pageable = pageable
        )

        val hits = hitSlice.content
        val items = loadItemsByHits(hits)

        val nextCursor = hits.lastOrNull()?.let { last ->
            CursorCodec.encode(
                SearchCursor(
                    sort = "relevance",
                    score = last.score,
                    createDate = last.createDate.toString(), // ✅ String
                    typeRank = last.typeRank,
                    id = last.id
                )
            )
        }

        return SearchListResponse(
            content = items,
            size = pageable.pageSize,
            hasNext = hitSlice.hasNext(),
            nextCursor = if (hitSlice.hasNext()) nextCursor else null
        )
    }

    // Step2: IN 조회 2번 + Step1 순서 유지
    private fun loadItemsByHits(hits: List<Any>): List<UnifiedSearchListItem> {
        if (hits.isEmpty()) return emptyList()

        // hits는 SearchHitRow 또는 SearchHitScoreRow가 올 수 있으므로 공통 속성 접근을 위해 캐스팅
        // (Kotlin에선 제네릭으로 깔끔하게 만들 수도 있는데, 여기선 간단히 처리)
        val typed = hits.map {
            when (it) {
                is com.back.domain.search.search.service.projection.SearchHitRow ->
                    Triple(it.type, it.id, it)
                is com.back.domain.search.search.service.projection.SearchHitScoreRow ->
                    Triple(it.type, it.id, it)
                else -> throw IllegalStateException("Unknown hit row type: ${it::class.qualifiedName}")
            }
        }

        val auctionIds = typed.filter { it.first == "AUCTION" }.map { it.second }
        val postIds = typed.filter { it.first == "POST" }.map { it.second }

        val auctionMap =
            if (auctionIds.isEmpty()) emptyMap()
            else auctionRepository.findListRowsByIds(auctionIds).associateBy { it.id }

        val postMap =
            if (postIds.isEmpty()) emptyMap()
            else postRepository.findListRowsByIds(postIds).associateBy { it.id }

        return typed.mapNotNull { (type, id, _) ->
            when (type) {
                "AUCTION" -> auctionMap[id]?.let { a ->
                    UnifiedSearchListItem(a.id, "AUCTION", a.title, a.price, a.status)
                }
                "POST" -> postMap[id]?.let { p ->
                    UnifiedSearchListItem(p.id, "POST", p.title, p.price, p.status)
                }
                else -> null
            }
        }
    }
}
