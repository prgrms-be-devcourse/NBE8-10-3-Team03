package com.back.domain.search.searchdocument.init

import com.back.domain.search.searchdocument.service.SearchDocumentMvBatchService
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class SearchDocumentMvInitializer(
    private val batchService: SearchDocumentMvBatchService
) {

    @PostConstruct
    fun init() {
        batchService.refreshAll()
    }
}