package com.back.domain.search.search.controller

import com.back.domain.search.search.dto.UnifiedSearchResponse
import com.back.domain.search.search.service.SearchService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class SearchControllerTest {
    @Test
    @DisplayName("검색 컨트롤러는 서비스 결과를 RsData/SearchResponse 로 매핑한다.")
    fun t1() {
        val searchService = Mockito.mock(SearchService::class.java)
        val controller = SearchController(searchService)

        val pageable = PageRequest.of(0, 10)
        val content = listOf(
            UnifiedSearchResponse(
                id = 10,
                type = "POST",
                title = "검색결과",
                price = 10000,
                status = "SALE",
                categoryName = "디지털",
                thumbnailUrl = null,
                createDate = LocalDateTime.now()
            )
        )

        Mockito.`when`(searchService.searchUnified("노트북", pageable))
            .thenReturn(PageImpl(content, pageable, 1))

        val rsData = controller.search("노트북", pageable)

        assertThat(rsData.resultCode).isEqualTo("200-1")
        assertThat(rsData.msg).isEqualTo("검색이 완료되었습니다.")
        assertThat(rsData.data!!.content).hasSize(1)
        assertThat(rsData.data!!.totalElements).isEqualTo(1)
    }
}
