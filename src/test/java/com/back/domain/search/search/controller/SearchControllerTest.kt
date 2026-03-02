package com.back.domain.search.search.controller

import com.back.domain.search.search.dto.SearchResponse
import com.back.domain.search.search.dto.UnifiedSearchResponse
import com.back.domain.search.search.service.SearchService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime

class SearchControllerTest {

    @Test
    @DisplayName("검색 컨트롤러는 서비스 결과를 RsData<SearchResponse>로 매핑한다.")
    fun t1() {
        val searchService = Mockito.mock(SearchService::class.java)
        val controller = SearchController(searchService)

        val fixedNow = LocalDateTime.of(2026, 3, 2, 12, 0)

        val content = listOf(
            UnifiedSearchResponse(
                id = 10,
                type = "POST",
                title = "검색결과",
                price = 10000,
                status = "SALE",
                categoryName = "디지털",
                thumbnailUrl = null,
                createDate = fixedNow
            )
        )

        val result = SearchResponse.ofCursor(
            content = content,
            size = 20,
            hasNext = true,
            nextCursor = "CURSOR_NEXT"
        )

        Mockito.`when`(
            searchService.searchUnifiedCursor(
                keyword = "노트북",
                sort = "relevance",
                size = 20,
                cursor = null
            )
        ).thenReturn(result)

        val rsData = controller.search(
            keyword = "노트북",
            sort = "relevance",
            size = 20,
            cursor = null
        )

        assertThat(rsData.resultCode).isEqualTo("200-1")
        assertThat(rsData.msg).isEqualTo("검색이 완료되었습니다.")

        val data = rsData.data!!
        assertThat(data.content).hasSize(1)
        assertThat(data.page).isEqualTo(0)
        assertThat(data.size).isEqualTo(20)
        assertThat(data.hasNext).isTrue()
        assertThat(data.nextCursor).isEqualTo("CURSOR_NEXT")
    }

    @Test
    @DisplayName("size는 1~50으로 보정된다 (0이면 1, 999면 50)")
    fun t2() {
        val searchService = Mockito.mock(SearchService::class.java)
        val controller = SearchController(searchService)

        val empty1 = SearchResponse.ofCursor(
            content = emptyList(),
            size = 1,
            hasNext = false,
            nextCursor = "IGNORED" // hasNext=false면 null로 내려가야 함
        )
        Mockito.`when`(
            searchService.searchUnifiedCursor("노트북", "relevance", 1, null)
        ).thenReturn(empty1)

        val rs1 = controller.search(keyword = "노트북", sort = "relevance", size = 0, cursor = null)
        assertThat(rs1.data!!.size).isEqualTo(1)
        assertThat(rs1.data!!.nextCursor).isNull()

        val empty50 = SearchResponse.ofCursor(
            content = emptyList(),
            size = 50,
            hasNext = false,
            nextCursor = null
        )
        Mockito.`when`(
            searchService.searchUnifiedCursor("노트북", "relevance", 50, null)
        ).thenReturn(empty50)

        val rs2 = controller.search(keyword = "노트북", sort = "relevance", size = 999, cursor = null)
        assertThat(rs2.data!!.size).isEqualTo(50)

        Mockito.verify(searchService).searchUnifiedCursor("노트북", "relevance", 1, null)
        Mockito.verify(searchService).searchUnifiedCursor("노트북", "relevance", 50, null)
    }

    @Test
    @DisplayName("sort는 relevance/newest/oldest만 허용하며 그 외는 relevance로 보정된다.")
    fun t3() {
        val searchService = Mockito.mock(SearchService::class.java)
        val controller = SearchController(searchService)

        val result = SearchResponse.ofCursor(
            content = emptyList(),
            size = 20,
            hasNext = false,
            nextCursor = null
        )

        Mockito.`when`(
            searchService.searchUnifiedCursor("노트북", "relevance", 20, null)
        ).thenReturn(result)

        controller.search(keyword = "노트북", sort = "WeIrD", size = 20, cursor = null)

        Mockito.verify(searchService).searchUnifiedCursor("노트북", "relevance", 20, null)
    }

    @Test
    @DisplayName("cursor는 그대로 서비스에 전달된다.")
    fun t4() {
        val searchService = Mockito.mock(SearchService::class.java)
        val controller = SearchController(searchService)

        val result = SearchResponse.ofCursor(
            content = emptyList(),
            size = 10,
            hasNext = true,
            nextCursor = "NEXT"
        )

        Mockito.`when`(
            searchService.searchUnifiedCursor("노트북", "newest", 10, "CURSOR123")
        ).thenReturn(result)

        val rs = controller.search(keyword = "노트북", sort = "newest", size = 10, cursor = "CURSOR123")

        Mockito.verify(searchService).searchUnifiedCursor("노트북", "newest", 10, "CURSOR123")
        assertThat(rs.data!!.nextCursor).isEqualTo("NEXT")
    }
}