package com.back.domain.search.searchDocument.repository

import com.back.domain.search.search.service.projection.UnifiedSearchRow
import com.back.domain.search.searchDocument.entity.SearchDocumentMv
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface SearchDocumentMvRepository : JpaRepository<SearchDocumentMv, Long> {

    @Query(
        value = """
        SELECT
          sd.id AS id,
          sd.document_type AS type,
          sd.title AS title,
          sd.price AS price,
          sd.status AS status,
          NULL AS statusDisplayName,
          sd.category_id AS categoryId,
          sd.thumbnail_url AS thumbnailUrl,
          sd.created_at AS createDate,
          MATCH(sd.title, sd.content) AGAINST (:kw IN BOOLEAN MODE) AS score
        FROM search_document_mv sd
        WHERE sd.deleted = 0
          AND MATCH(sd.title, sd.content) AGAINST (:kw IN BOOLEAN MODE)
          AND (
            :cursorScore IS NULL OR
            MATCH(sd.title, sd.content) AGAINST (:kw IN BOOLEAN MODE) < :cursorScore OR
            (
              MATCH(sd.title, sd.content) AGAINST (:kw IN BOOLEAN MODE) = :cursorScore
              AND sd.created_at < :cursorCreateDate
            ) OR
            (
              MATCH(sd.title, sd.content) AGAINST (:kw IN BOOLEAN MODE) = :cursorScore
              AND sd.created_at = :cursorCreateDate
              AND sd.id < :cursorId
            )
          )
        ORDER BY score DESC, sd.created_at DESC, sd.id DESC
        """,
        nativeQuery = true
    )
    fun searchRelevanceCursor(
        @Param("kw") kw: String,
        @Param("cursorScore") cursorScore: Double?,
        @Param("cursorCreateDate") cursorCreateDate: LocalDateTime?,
        @Param("cursorId") cursorId: Int?,
        pageable: Pageable
    ): Slice<UnifiedSearchRow>

    @Query(
        value = """
        SELECT
          sd.id AS id,
          sd.document_type AS type,
          sd.title AS title,
          sd.price AS price,
          sd.status AS status,
          NULL AS statusDisplayName,
          sd.category_id AS categoryId,
          sd.thumbnail_url AS thumbnailUrl,
          sd.created_at AS createDate,
          NULL AS score
        FROM search_document_mv sd
        WHERE sd.deleted = 0
          AND MATCH(sd.title, sd.content) AGAINST (:kw IN BOOLEAN MODE)
          AND (
            :cursorCreateDate IS NULL OR
            sd.created_at < :cursorCreateDate OR
            (sd.created_at = :cursorCreateDate AND sd.id < :cursorId)
          )
        ORDER BY sd.created_at DESC, sd.id DESC
        """,
        nativeQuery = true
    )
    fun searchNewestCursor(
        @Param("kw") kw: String,
        @Param("cursorCreateDate") cursorCreateDate: LocalDateTime?,
        @Param("cursorId") cursorId: Int?,
        pageable: Pageable
    ): Slice<UnifiedSearchRow>

    @Query(
        value = """
        SELECT
          sd.id AS id,
          sd.document_type AS type,
          sd.title AS title,
          sd.price AS price,
          sd.status AS status,
          NULL AS statusDisplayName,
          sd.category_id AS categoryId,
          sd.thumbnail_url AS thumbnailUrl,
          sd.created_at AS createDate,
          NULL AS score
        FROM search_document_mv sd
        WHERE sd.deleted = 0
          AND MATCH(sd.title, sd.content) AGAINST (:kw IN BOOLEAN MODE)
          AND (
            :cursorCreateDate IS NULL OR
            sd.created_at > :cursorCreateDate OR
            (sd.created_at = :cursorCreateDate AND sd.id > :cursorId)
          )
        ORDER BY sd.created_at ASC, sd.id ASC
        """,
        nativeQuery = true
    )
    fun searchOldestCursor(
        @Param("kw") kw: String,
        @Param("cursorCreateDate") cursorCreateDate: LocalDateTime?,
        @Param("cursorId") cursorId: Int?,
        pageable: Pageable
    ): Slice<UnifiedSearchRow>
}