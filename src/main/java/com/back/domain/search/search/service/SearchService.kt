package com.back.domain.search.search.service

import com.back.domain.search.search.dto.SearchResponse
import com.back.domain.search.search.dto.UnifiedSearchResponse
import com.back.domain.search.search.service.projection.UnifiedSearchRow
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Base64

@Service
class SearchService(
    private val searchDocumentMvQueryService: SearchDocumentMvQueryService
) {

    fun searchUnifiedCursor(
        keyword: String,
        sort: String,
        size: Int,
        cursor: String?
    ): SearchResponse {
        return when (sort) {
            "newest" -> searchNewest(keyword, size, cursor)
            "oldest" -> searchOldest(keyword, size, cursor)
            else -> searchRelevance(keyword, size, cursor)
        }
    }

    private fun searchRelevance(
        keyword: String,
        size: Int,
        cursor: String?
    ): SearchResponse {
        val decoded = RelevanceCursor.decode(cursor)

        val slice = searchDocumentMvQueryService.searchRelevance(
            keyword = keyword,
            cursorScore = decoded?.score,
            cursorCreateDate = decoded?.createDate,
            cursorId = decoded?.id,
            size = size
        )

        val items = slice.content.map { it.toResponse() }

        val nextCursor = if (slice.hasNext() && items.isNotEmpty()) {
            val last = items.last()
            RelevanceCursor(
                score = last.score ?: 0.0,
                createDate = last.createDate,
                id = last.id
            ).encode()
        } else {
            null
        }

        return SearchResponse(
            content = items,
            page = slice.number,
            size = slice.size,
            hasNext = slice.hasNext(),
            nextCursor = nextCursor
        )
    }

    private fun searchNewest(
        keyword: String,
        size: Int,
        cursor: String?
    ): SearchResponse {
        val decoded = DateCursor.decode(cursor)

        val slice = searchDocumentMvQueryService.searchNewest(
            keyword = keyword,
            cursorCreateDate = decoded?.createDate,
            cursorId = decoded?.id,
            size = size
        )

        val items = slice.content.map { it.toResponse() }

        val nextCursor = if (slice.hasNext() && items.isNotEmpty()) {
            val last = items.last()
            DateCursor(
                createDate = last.createDate,
                id = last.id
            ).encode()
        } else {
            null
        }

        return SearchResponse(
            content = items,
            page = slice.number,
            size = slice.size,
            hasNext = slice.hasNext(),
            nextCursor = nextCursor
        )
    }

    private fun searchOldest(
        keyword: String,
        size: Int,
        cursor: String?
    ): SearchResponse {
        val decoded = DateCursor.decode(cursor)

        val slice = searchDocumentMvQueryService.searchOldest(
            keyword = keyword,
            cursorCreateDate = decoded?.createDate,
            cursorId = decoded?.id,
            size = size
        )

        val items = slice.content.map { it.toResponse() }

        val nextCursor = if (slice.hasNext() && items.isNotEmpty()) {
            val last = items.last()
            DateCursor(
                createDate = last.createDate,
                id = last.id
            ).encode()
        } else {
            null
        }

        return SearchResponse(
            content = items,
            page = slice.number,
            size = slice.size,
            hasNext = slice.hasNext(),
            nextCursor = nextCursor
        )
    }

    private fun UnifiedSearchRow.toResponse(): UnifiedSearchResponse {
        return UnifiedSearchResponse(
            id = id,
            type = type,
            title = title,
            price = price,
            status = status,
            statusDisplayName = statusDisplayName,
            categoryId = categoryId,
            thumbnailUrl = thumbnailUrl,
            createDate = createDate,
            score = score
        )
    }
}