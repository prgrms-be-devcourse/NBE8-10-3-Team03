package com.back.domain.search.search.service.port

import com.back.domain.search.search.dto.UnifiedSearchResponse
import org.springframework.data.domain.Pageable

interface SearchProvider {
    fun search(keyword: String, pageable: Pageable): List<UnifiedSearchResponse>
}