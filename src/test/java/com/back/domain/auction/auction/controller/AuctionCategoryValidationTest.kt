package com.back.domain.auction.auction.controller

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
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

    private fun requestPart(json: String): MockMultipartFile =
        MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, json.trimIndent().toByteArray())

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 등록 실패 - 존재하지 않는 카테고리")
    @Throws(Exception::class)
    fun t1() {
        val request = requestPart(
            """
            {
                "name": "카테고리 검증 경매",
                "description": "카테고리 미존재 검증용 설명입니다.",
                "startPrice": 10000,
                "buyNowPrice": 15000,
                "categoryId": 999999,
                "durationHours": 24
            }
            """
        )

        mvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/auctions")
                .file(request)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-2"))
    }
}
