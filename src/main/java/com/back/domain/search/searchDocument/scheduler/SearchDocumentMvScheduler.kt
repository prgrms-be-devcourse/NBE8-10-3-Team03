package com.back.domain.search.searchdocument.scheduler

import com.back.domain.search.searchdocument.service.SearchDocumentMvBatchService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SearchDocumentMvScheduler(
    private val batchService: SearchDocumentMvBatchService
) {

    // 5분마다
    @Scheduled(cron = "0 */5 * * * *")
    fun refreshSearchDocumentMv() {
        batchService.refreshAll()
    }
}