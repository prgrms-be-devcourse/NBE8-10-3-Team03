package com.back.domain.search.searchdocument.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchDocumentMvBatchService(
    private val jdbcTemplate: JdbcTemplate
) {

    @Transactional
    fun refreshAll() {
        // 1) next 테이블 비우기
        jdbcTemplate.execute("TRUNCATE TABLE search_document_mv_next")

        // 2) auction 적재
        jdbcTemplate.execute(
            """
            INSERT INTO search_document_mv_next (
                document_type,
                source_id,
                title,
                content,
                price,
                status,
                category_id,
                thumbnail_url,
                created_at,
                deleted
            )
            SELECT
                'AUCTION',
                a.id,
                a.name,
                a.description,
                a.start_price,
                a.status,
                a.category_id,
                a.thumbnail_url,
                a.create_date,
                0
            FROM auction a
            """.trimIndent()
        )

        // 3) post 적재
        jdbcTemplate.execute(
            """
            INSERT INTO search_document_mv_next (
                document_type,
                source_id,
                title,
                content,
                price,
                status,
                category_id,
                thumbnail_url,
                created_at,
                deleted
            )
            SELECT
                'POST',
                p.id,
                p.title,
                p.content,
                p.price,
                p.status,
                p.category_id,
                p.thumbnail_url,
                p.create_date,
                p.deleted
            FROM post p
            """.trimIndent()
        )

        // 4) swap
        jdbcTemplate.execute(
            """
            RENAME TABLE
              search_document_mv TO search_document_mv_old,
              search_document_mv_next TO search_document_mv,
              search_document_mv_old TO search_document_mv_next
            """.trimIndent()
        )
    }
}