package com.back.domain.search.search.service

import com.back.domain.search.search.service.projection.UnifiedSearchRow
import com.back.domain.search.searchDocument.repository.SearchDocumentMvRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SearchDocumentMvQueryService(
    private val searchDocumentMvRepository: SearchDocumentMvRepository
) {

    fun searchRelevance(
        keyword: String,
        cursorScore: Double?,
        cursorCreateDate: LocalDateTime?,
        cursorId: Int?,
        size: Int
    ) = searchDocumentMvRepository.searchRelevanceCursor(
        kw = keyword,
        cursorScore = cursorScore,
        cursorCreateDate = cursorCreateDate,
        cursorId = cursorId,
        pageable = PageRequest.of(0, size)
    )

    fun searchNewest(
        keyword: String,
        cursorCreateDate: LocalDateTime?,
        cursorId: Int?,
        size: Int
    ) = searchDocumentMvRepository.searchNewestCursor(
        kw = keyword,
        cursorCreateDate = cursorCreateDate,
        cursorId = cursorId,
        pageable = PageRequest.of(0, size)
    )

    fun searchOldest(
        keyword: String,
        cursorCreateDate: LocalDateTime?,
        cursorId: Int?,
        size: Int
    ) = searchDocumentMvRepository.searchOldestCursor(
        kw = keyword,
        cursorCreateDate = cursorCreateDate,
        cursorId = cursorId,
        pageable = PageRequest.of(0, size)
    )
}