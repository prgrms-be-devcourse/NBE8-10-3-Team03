package com.back.domain.search.searchDocument.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "search_document_mv",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_search_document_mv_type_source",
            columnNames = ["document_type", "source_id"]
        )
    ]
)
class SearchDocumentMv(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "document_type", nullable = false, length = 20)
    var documentType: String,

    @Column(name = "source_id", nullable = false)
    var sourceId: Long,

    @Column(name = "title", nullable = false, length = 255)
    var title: String,

    @Column(name = "content", columnDefinition = "TEXT")
    var content: String? = null,

    @Column(name = "price")
    var price: Int? = null,

    @Column(name = "status", length = 30)
    var status: String? = null,

    @Column(name = "category_id")
    var categoryId: Long? = null,

    @Column(name = "thumbnail_url", length = 500)
    var thumbnailUrl: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false
)