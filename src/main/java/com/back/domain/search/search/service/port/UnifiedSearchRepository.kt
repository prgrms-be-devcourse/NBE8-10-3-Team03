package com.back.domain.search.search.service.port

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.search.search.service.projection.UnifiedSearchRow
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface UnifiedSearchRepository : JpaRepository<Auction, Int> {

    @Query(
        value = """
    SELECT
      t.id, t.type, t.title, t.price, t.status, t.statusDisplayName,
      t.categoryName, t.thumbnailUrl, t.createDate
    FROM (
      (
        SELECT
          a.id            AS id,
          'AUCTION'       AS type,
          a.name          AS title,
          a.start_price   AS price,
          a.status        AS status,
          NULL            AS statusDisplayName,
          c1.name         AS categoryName,
          a.thumbnail_url AS thumbnailUrl,
          a.create_date   AS createDate,
          MATCH(a.name, a.description) AGAINST (:kw IN BOOLEAN MODE) AS score
        FROM auction a
        JOIN categories c1 ON c1.id = a.category_id
        WHERE MATCH(a.name, a.description) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY score DESC, a.create_date DESC
        LIMIT 300
      )
      UNION ALL
      (
        SELECT
          p.id            AS id,
          'POST'          AS type,
          p.title         AS title,
          p.price         AS price,
          p.status        AS status,
          NULL            AS statusDisplayName,
          c2.name         AS categoryName,
          p.thumbnail_url AS thumbnailUrl,
          p.create_date   AS createDate,
          MATCH(p.title, p.content) AGAINST (:kw IN BOOLEAN MODE) AS score
        FROM post p
        JOIN categories c2 ON c2.id = p.category_id
        WHERE p.deleted = 0
          AND MATCH(p.title, p.content) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY score DESC, p.create_date DESC
        LIMIT 300
      )
    ) t
    ORDER BY
      -- 1) relevance면 score 우선
      CASE WHEN :sort = 'relevance' THEN t.score END DESC,
      -- 2) newest/oldest는 createDate로만
      CASE WHEN :sort = 'newest' THEN t.createDate END DESC,
      CASE WHEN :sort = 'oldest' THEN t.createDate END ASC,
      -- 3) tie-breaker(일관성)
      t.createDate DESC
  """,
        nativeQuery = true
    )
    fun searchUnifiedFulltext(
        @Param("kw") kw: String,
        @Param("sort") sort: String,
        pageable: Pageable
    ): Slice<UnifiedSearchRow>

    @Query(
        value = """
    SELECT
      t.id, t.type, t.title, t.price, t.status, t.statusDisplayName,
      t.categoryName, t.thumbnailUrl, t.createDate
    FROM (
      (
        SELECT
          a.id            AS id,
          'AUCTION'       AS type,
          a.name          AS title,
          a.start_price   AS price,
          a.status        AS status,
          NULL            AS statusDisplayName,
          c1.name         AS categoryName,
          a.thumbnail_url AS thumbnailUrl,
          a.create_date   AS createDate
          MATCH(a.name, a.description) AGAINST (:kw IN BOOLEAN MODE) AS score
        FROM auction a
        JOIN categories c1 ON c1.id = a.category_id
        WHERE MATCH(a.name, a.description) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY score DESC, a.create_date DESC
        LIMIT 300
      )
      UNION ALL
      (
        SELECT
          p.id            AS id,
          'POST'          AS type,
          p.title         AS title,
          p.price         AS price,
          p.status        AS status,
          NULL            AS statusDisplayName,
          c2.name         AS categoryName,
          p.thumbnail_url AS thumbnailUrl,
          p.create_date   AS createDate
          MATCH(p.title, p.content) AGAINST (:kw IN BOOLEAN MODE) AS score
        FROM post p
        JOIN categories c2 ON c2.id = p.category_id
        WHERE p.deleted = 0
          AND MATCH(p.title, p.content) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY score DESC, p.create_date DESC
        LIMIT 300
      )
    ) t
    ORDER BY t.score DESC, t.createDate DESC
  """,
        nativeQuery = true
    )
    fun searchUnifiedRelevance(@Param("kw") kw: String, pageable: Pageable): Slice<UnifiedSearchRow>

    @Query(
        value = """
    SELECT
      t.id, t.type, t.title, t.price, t.status, t.statusDisplayName,
      t.categoryName, t.thumbnailUrl, t.createDate
    FROM (
      (
        SELECT
          a.id            AS id,
          'AUCTION'       AS type,
          a.name          AS title,
          a.start_price   AS price,
          a.status        AS status,
          NULL            AS statusDisplayName,
          c1.name         AS categoryName,
          a.thumbnail_url AS thumbnailUrl,
          a.create_date   AS createDate
        FROM auction a
        JOIN categories c1 ON c1.id = a.category_id
        WHERE MATCH(a.name, a.description) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY a.create_date DESC
        LIMIT 200
      )
      UNION ALL
      (
        SELECT
          p.id            AS id,
          'POST'          AS type,
          p.title         AS title,
          p.price         AS price,
          p.status        AS status,
          NULL            AS statusDisplayName,
          c2.name         AS categoryName,
          p.thumbnail_url AS thumbnailUrl,
          p.create_date   AS createDate
        FROM post p
        JOIN categories c2 ON c2.id = p.category_id
        WHERE p.deleted = 0
          AND MATCH(p.title, p.content) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY p.create_date DESC
        LIMIT 200
      )
    ) t
    ORDER BY t.createDate DESC
  """,
        nativeQuery = true
    )
    fun searchUnifiedNewest(@Param("kw") kw: String, pageable: Pageable): Slice<UnifiedSearchRow>

    @Query(
        value = """
    SELECT
      t.id, t.type, t.title, t.price, t.status, t.statusDisplayName,
      t.categoryName, t.thumbnailUrl, t.createDate
    FROM (
      (
        SELECT
          a.id            AS id,
          'AUCTION'       AS type,
          a.name          AS title,
          a.start_price   AS price,
          a.status        AS status,
          NULL            AS statusDisplayName,
          c1.name         AS categoryName,
          a.thumbnail_url AS thumbnailUrl,
          a.create_date   AS createDate
        FROM auction a
        JOIN categories c1 ON c1.id = a.category_id
        WHERE MATCH(a.name, a.description) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY a.create_date ASC
        LIMIT 200
      )
      UNION ALL
      (
        SELECT
          p.id            AS id,
          'POST'          AS type,
          p.title         AS title,
          p.price         AS price,
          p.status        AS status,
          NULL            AS statusDisplayName,
          c2.name         AS categoryName,
          p.thumbnail_url AS thumbnailUrl,
          p.create_date   AS createDate
        FROM post p
        JOIN categories c2 ON c2.id = p.category_id
        WHERE p.deleted = 0
          AND MATCH(p.title, p.content) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY p.create_date ASC
        LIMIT 200
      )
    ) t
    ORDER BY t.createDate ASC
  """,
        nativeQuery = true
    )
    fun searchUnifiedOldest(@Param("kw") kw: String, pageable: Pageable): Slice<UnifiedSearchRow>

    @Query(
        value = """
    SELECT
      t.id, t.type, t.title, t.price, t.status, t.statusDisplayName,
      t.categoryName, t.thumbnailUrl, t.createDate,
      t.score, t.typeRank
    FROM (
      (
        SELECT
          a.id            AS id,
          'AUCTION'       AS type,
          1               AS typeRank,
          a.name          AS title,
          a.start_price   AS price,
          a.status        AS status,
          NULL            AS statusDisplayName,
          c1.name         AS categoryName,
          a.thumbnail_url AS thumbnailUrl,
          a.create_date   AS createDate,
          MATCH(a.name, a.description) AGAINST (:kw IN BOOLEAN MODE) AS score
        FROM auction a
        JOIN categories c1 ON c1.id = a.category_id
        WHERE MATCH(a.name, a.description) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY score DESC, a.create_date DESC, a.id DESC
        LIMIT 300
      )
      UNION ALL
      (
        SELECT
          p.id            AS id,
          'POST'          AS type,
          0               AS typeRank,
          p.title         AS title,
          p.price         AS price,
          p.status        AS status,
          NULL            AS statusDisplayName,
          c2.name         AS categoryName,
          p.thumbnail_url AS thumbnailUrl,
          p.create_date   AS createDate,
          MATCH(p.title, p.content) AGAINST (:kw IN BOOLEAN MODE) AS score
        FROM post p
        JOIN categories c2 ON c2.id = p.category_id
        WHERE p.deleted = 0
          AND MATCH(p.title, p.content) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY score DESC, p.create_date DESC, p.id DESC
        LIMIT 300
      )
    ) t
    WHERE (
      :cursorScore IS NULL OR
      t.score < :cursorScore OR
      (t.score = :cursorScore AND t.createDate < :cursorCreateDate) OR
      (t.score = :cursorScore AND t.createDate = :cursorCreateDate AND t.typeRank < :cursorTypeRank) OR
      (t.score = :cursorScore AND t.createDate = :cursorCreateDate AND t.typeRank = :cursorTypeRank AND t.id < :cursorId)
    )
    ORDER BY t.score DESC, t.createDate DESC, t.typeRank DESC, t.id DESC
    """,
        nativeQuery = true
    )
    fun searchRelevanceCursor(
        @Param("kw") kw: String,
        @Param("cursorScore") cursorScore: Double?,
        @Param("cursorCreateDate") cursorCreateDate: LocalDateTime?,
        @Param("cursorTypeRank") cursorTypeRank: Int?,
        @Param("cursorId") cursorId: Int?,
        pageable: Pageable
    ): Slice<UnifiedSearchRow>

    @Query(
        value = """
    SELECT
      t.id, t.type, t.title, t.price, t.status, t.statusDisplayName,
      t.categoryName, t.thumbnailUrl, t.createDate,
      NULL AS score, t.typeRank
    FROM (
      (
        SELECT
          a.id AS id, 
          'AUCTION' AS type, 
          1 AS typeRank,
          a.name AS title, 
          a.start_price AS price, 
          a.status AS status,
          NULL AS statusDisplayName, 
          c1.name AS categoryName,
          a.thumbnail_url AS thumbnailUrl, 
          a.create_date AS createDate
        FROM auction a
        JOIN categories c1 ON c1.id = a.category_id
        WHERE MATCH(a.name, a.description) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY a.create_date DESC, a.id DESC
        LIMIT 200
      )
      UNION ALL
      (
        SELECT
          p.id AS id, 'POST' AS type, 0 AS typeRank,
          p.title AS title, p.price AS price, p.status AS status,
          NULL AS statusDisplayName, c2.name AS categoryName,
          p.thumbnail_url AS thumbnailUrl, p.create_date AS createDate
        FROM post p
        JOIN categories c2 ON c2.id = p.category_id
        WHERE p.deleted = 0
          AND MATCH(p.title, p.content) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY p.create_date DESC, p.id DESC
        LIMIT 200
      )
    ) t
    WHERE (
      :cursorCreateDate IS NULL OR
      t.createDate < :cursorCreateDate OR
      (t.createDate = :cursorCreateDate AND t.typeRank < :cursorTypeRank) OR
      (t.createDate = :cursorCreateDate AND t.typeRank = :cursorTypeRank AND t.id < :cursorId)
    )
    ORDER BY t.createDate DESC, t.typeRank DESC, t.id DESC
  """,
        nativeQuery = true
    )
    fun searchNewestCursor(
        @Param("kw") kw: String,
        @Param("cursorCreateDate") cursorCreateDate: LocalDateTime?,
        @Param("cursorTypeRank") cursorTypeRank: Int?,
        @Param("cursorId") cursorId: Int?,
        pageable: Pageable
    ): Slice<UnifiedSearchRow>

    @Query(
        value = """
    SELECT
      t.id, 
      t.type, 
      t.title, 
      t.price, 
      t.status, 
      t.statusDisplayName,
      t.categoryName, 
      t.thumbnailUrl, 
      t.createDate,
      NULL AS score, 
      t.typeRank
    FROM (
      (
        SELECT
          a.id AS id, 'AUCTION' AS type, 1 AS typeRank,
          a.name AS title, a.start_price AS price, a.status AS status,
          NULL AS statusDisplayName, c1.name AS categoryName,
          a.thumbnail_url AS thumbnailUrl, a.create_date AS createDate
        FROM auction a
        JOIN categories c1 ON c1.id = a.category_id
        WHERE MATCH(a.name, a.description) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY a.create_date DESC, a.id DESC
        LIMIT 200
      )
      UNION ALL
      (
        SELECT
          p.id AS id,
          'POST' AS type, 
          0 AS typeRank,
          p.title AS title, 
          p.price AS price, 
          p.status AS status,
          NULL AS statusDisplayName, 
          c2.name AS categoryName,
          p.thumbnail_url AS thumbnailUrl, 
          p.create_date AS createDate
        FROM post p
        JOIN categories c2 ON c2.id = p.category_id
        WHERE p.deleted = 0
          AND MATCH(p.title, p.content) AGAINST (:kw IN BOOLEAN MODE)
        ORDER BY p.create_date DESC, p.id DESC
        LIMIT 200
      )
    ) t
    WHERE (
      :cursorCreateDate IS NULL OR
      t.createDate > :cursorCreateDate OR
      (t.createDate = :cursorCreateDate AND t.typeRank > :cursorTypeRank) OR
      (t.createDate = :cursorCreateDate AND t.typeRank = :cursorTypeRank AND t.id > :cursorId)
    )
    ORDER BY t.createDate ASC, t.typeRank ASC, t.id ASC
  """,
        nativeQuery = true
    )
    fun searchOldestCursor(
        @Param("kw") kw: String,
        @Param("cursorCreateDate") cursorCreateDate: LocalDateTime?,
        @Param("cursorTypeRank") cursorTypeRank: Int?,
        @Param("cursorId") cursorId: Int?,
        pageable: Pageable
    ): Slice<UnifiedSearchRow>

}