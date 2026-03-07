package com.back.domain.search.search.service

import com.back.domain.search.search.dto.SearchCursor
import com.back.domain.search.search.dto.SearchResponse
import com.back.domain.search.search.dto.UnifiedSearchResponse
import com.back.domain.search.search.service.port.UnifiedSearchRepository
import com.back.domain.search.search.service.projection.CursorCodec
import com.back.domain.search.search.service.projection.UnifiedSearchRow
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SearchService(
    /*
        private val postRepository: PostRepository,
        private val auctionRepository: AuctionRepository

     */
    //private val searchProviders: List<SearchProvider>
    private val unifiedSearchRepository: UnifiedSearchRepository
) {

    // 통합 검색 (POST + AUCTION)
    fun searchUnified(keyword: String, sort: String, pageable: Pageable): Slice<UnifiedSearchResponse> {
        val kw = toBooleanKw(keyword)
        val p = PageRequest.of(pageable.pageNumber, pageable.pageSize) // Sort 제거

        val rows = when (sort.lowercase()) {
            "newest" -> unifiedSearchRepository.searchUnifiedNewest(kw, p)
            "oldest" -> unifiedSearchRepository.searchUnifiedOldest(kw, p)
            else -> unifiedSearchRepository.searchUnifiedRelevance(kw, p) // default relevance
        }

        return rows.map { it.toDto() }
    }

    fun searchUnifiedCursor(keyword: String, sort: String, size: Int, cursor: String?): SearchResponse {
        val kw = toBooleanKw(keyword)
        val pageable = PageRequest.of(0, size.coerceIn(1, 50))

        val cur = cursor?.let { CursorCodec.decode(it) }

        val slice = when (sort.lowercase()) {
            "newest" -> unifiedSearchRepository.searchNewestCursor(
                kw,
                cur?.createDate, cur?.typeRank, cur?.id,
                pageable
            )
            "oldest" -> unifiedSearchRepository.searchOldestCursor(
                kw,
                cur?.createDate, cur?.typeRank, cur?.id,
                pageable
            )
            else -> unifiedSearchRepository.searchRelevanceCursor(
                kw,
                cur?.score, cur?.createDate, cur?.typeRank, cur?.id,
                pageable
            )
        }

        val content = slice.content.map { it.toDto() } // score/typeRank 버림

        val nextCursor = slice.content.lastOrNull()?.let { last ->
            val nc = SearchCursor(
                sort = sort.lowercase(),
                score = last.score,               // newest/oldest는 null
                createDate = last.createDate,
                typeRank = last.typeRank,
                id = last.id
            )
            CursorCodec.encode(nc)
        }

        return SearchResponse.ofCursor(
            content = content,
            size = pageable.pageSize,
            hasNext = slice.hasNext(),
            nextCursor = nextCursor
        )
    }

    private fun normalizeSort(sort: String): String =
        when (sort.lowercase()) {
            "relevance", "newest", "oldest" -> sort.lowercase()
            else -> "relevance"
        }

    fun toBooleanKw(input: String): String =
        input.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { "+${it}*" }

    private fun UnifiedSearchRow.toDto() = UnifiedSearchResponse(
        id = id,
        type = type,
        title = title,
        price = price,
        status = status,
        statusDisplayName = statusDisplayName,
        categoryId = categoryId,
        thumbnailUrl = thumbnailUrl,
        createDate = createDate
    )

//    fun searchUnifiedCursor(
//        keyword: String,
//        sort: String,
//        size: Int,
//        cursor: String?
//    ): SearchResponse {
//        return when (sort) {
//            "newest" -> searchNewest(keyword, size, cursor)
//            "oldest" -> searchOldest(keyword, size, cursor)
//            else -> searchRelevance(keyword, size, cursor)
//        }
//    }
//
//    private fun searchRelevance(
//        keyword: String,
//        size: Int,
//        cursor: String?
//    ): SearchResponse {
//        val decoded = RelevanceCursor.decode(cursor)
//
//        val slice = searchDocumentMvQueryService.searchRelevance(
//            keyword = keyword,
//            cursorScore = decoded?.score,
//            cursorCreateDate = decoded?.createDate,
//            cursorId = decoded?.id,
//            size = size
//        )
//
//        val items = slice.content.map { it.toResponse() }
//
//        val nextCursor = if (slice.hasNext() && items.isNotEmpty()) {
//            val last = items.last()
//            RelevanceCursor(
//                score = last.score ?: 0.0,
//                createDate = last.createDate,
//                id = last.id
//            ).encode()
//        } else {
//            null
//        }
//
//        return SearchResponse(
//            content = items,
//            page = slice.number,
//            size = slice.size,
//            hasNext = slice.hasNext(),
//            nextCursor = nextCursor
//        )
//    }
//
//    private fun searchNewest(
//        keyword: String,
//        size: Int,
//        cursor: String?
//    ): SearchResponse {
//        val decoded = DateCursor.decode(cursor)
//
//        val slice = searchDocumentMvQueryService.searchNewest(
//            keyword = keyword,
//            cursorCreateDate = decoded?.createDate,
//            cursorId = decoded?.id,
//            size = size
//        )
//
//        val items = slice.content.map { it.toResponse() }
//
//        val nextCursor = if (slice.hasNext() && items.isNotEmpty()) {
//            val last = items.last()
//            DateCursor(
//                createDate = last.createDate,
//                id = last.id
//            ).encode()
//        } else {
//            null
//        }
//
//        return SearchResponse(
//            content = items,
//            page = slice.number,
//            size = slice.size,
//            hasNext = slice.hasNext(),
//            nextCursor = nextCursor
//        )
//    }
//
//    private fun searchOldest(
//        keyword: String,
//        size: Int,
//        cursor: String?
//    ): SearchResponse {
//        val decoded = DateCursor.decode(cursor)
//
//        val slice = searchDocumentMvQueryService.searchOldest(
//            keyword = keyword,
//            cursorCreateDate = decoded?.createDate,
//            cursorId = decoded?.id,
//            size = size
//        )
//
//        val items = slice.content.map { it.toResponse() }
//
//        val nextCursor = if (slice.hasNext() && items.isNotEmpty()) {
//            val last = items.last()
//            DateCursor(
//                createDate = last.createDate,
//                id = last.id
//            ).encode()
//        } else {
//            null
//        }
//
//        return SearchResponse(
//            content = items,
//            page = slice.number,
//            size = slice.size,
//            hasNext = slice.hasNext(),
//            nextCursor = nextCursor
//        )
//    }
//
//    private fun UnifiedSearchRow.toResponse(): UnifiedSearchResponse {
//        return UnifiedSearchResponse(
//            id = id,
//            type = type,
//            title = title,
//            price = price,
//            status = status,
//            statusDisplayName = statusDisplayName,
//            categoryId = categoryId,
//            thumbnailUrl = thumbnailUrl,
//            createDate = createDate,
//            score = score
//        )
//    }
}