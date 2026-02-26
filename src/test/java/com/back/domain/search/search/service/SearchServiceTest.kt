package com.back.domain.search.search.service

import com.back.domain.search.search.dto.UnifiedSearchResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

class SearchServiceTest {
    private class StubProvider(
        private val results: List<UnifiedSearchResponse>
    ) : SearchProvider {
        var lastKeyword: String? = null
        var lastPageable: Pageable? = null

        override fun search(keyword: String, pageable: Pageable): List<UnifiedSearchResponse> {
            lastKeyword = keyword
            lastPageable = pageable
            return results
        }
    }

    @Test
    @DisplayName("통합 검색은 provider 결과를 createDate 내림차순으로 병합한다.")
    fun t1() {
        val older = searchItem(id = 1, title = "old", createDate = LocalDateTime.of(2025, 1, 1, 10, 0))
        val newer = searchItem(id = 2, title = "new", createDate = LocalDateTime.of(2025, 1, 1, 11, 0))

        val p1 = StubProvider(listOf(older))
        val p2 = StubProvider(listOf(newer))

        val searchService = SearchService(listOf(p1, p2))
        val result = searchService.searchUnified("camera", PageRequest.of(0, 10))

        assertThat(p1.lastKeyword).isEqualTo("camera")
        assertThat(p2.lastKeyword).isEqualTo("camera")
        assertThat(result.content.map { it.id }).containsExactly(2, 1)
        assertThat(result.totalElements).isEqualTo(2)
    }

    @Test
    @DisplayName("통합 검색 페이징은 offset/pageSize를 기준으로 슬라이스한다.")
    fun t2() {
        val items = listOf(
            searchItem(1, "a", LocalDateTime.of(2025, 1, 1, 10, 3)),
            searchItem(2, "b", LocalDateTime.of(2025, 1, 1, 10, 2)),
            searchItem(3, "c", LocalDateTime.of(2025, 1, 1, 10, 1)),
        )

        val provider = StubProvider(items)
        val searchService = SearchService(listOf(provider))
        val page = searchService.searchUnified("phone", PageRequest.of(1, 1))

        assertThat(page.content).hasSize(1)
        assertThat(page.content[0].id).isEqualTo(2)
        assertThat(page.totalElements).isEqualTo(3)
    }

    @Test
    @DisplayName("검색 provider가 비어 있으면 빈 페이지를 반환한다.")
    fun t3() {
        val searchService = SearchService(emptyList())
        val page = searchService.searchUnified("anything", PageRequest.of(0, 10))

        assertThat(page.content).isEmpty()
        assertThat(page.totalElements).isZero()
    }

    private fun searchItem(id: Int, title: String, createDate: LocalDateTime): UnifiedSearchResponse {
        return UnifiedSearchResponse(
            id = id,
            type = "POST",
            title = title,
            price = 1000,
            status = "SALE",
            categoryName = "전자기기",
            thumbnailUrl = null,
            createDate = createDate
        )
    }
}
