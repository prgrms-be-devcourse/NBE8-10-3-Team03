package com.back.domain.post.post.controller

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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostCategoryValidationTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Test
    @WithUserDetails("user1")
    @DisplayName("게시글 등록 실패 - 존재하지 않는 카테고리")
    @Throws(Exception::class)
    fun t1() {
        mvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/posts")
                .param("title", "카테고리 검증")
                .param("content", "카테고리 미존재 검증을 위한 게시글 내용입니다.")
                .param("price", "10000")
                .param("categoryId", "999999")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-2"))
    }
}
