package com.back.domain.auction.auction.controller

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuctionCategoryValidationTest {
    @Autowired
    private lateinit var mvc: MockMvc

    private fun applyParams(
        builder: MockMultipartHttpServletRequestBuilder,
        params: Map<String, String>
    ): MockMultipartHttpServletRequestBuilder =
        params.entries.fold(builder) { acc, (key, value) -> acc.param(key, value) }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 등록 실패 - 존재하지 않는 카테고리")
    @Throws(Exception::class)
    fun t1() {
        val request = mapOf(
            "name" to "카테고리 검증 경매",
            "description" to "카테고리 미존재 검증용 설명입니다.",
            "startPrice" to "10000",
            "buyNowPrice" to "15000",
            "categoryId" to "999999",
            "durationHours" to "24"
        )

        mvc.perform(
            applyParams(
                MockMvcRequestBuilders.multipart("/api/v1/auctions"),
                request
            )
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-2"))
    }
}
