package com.back.domain.auction.auction.service.adapter

import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.repository.CategoryRepository
import com.back.domain.category.category.service.port.CategoryPort
import com.back.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class CategoryAdapterTest {
    @Autowired
    private lateinit var categoryPort: CategoryPort

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Test
    @DisplayName("카테고리 ID로 조회 성공")
    fun t1() {
        val savedCategory = categoryRepository.save(Category("테스트 카테고리"))

        val foundCategory = categoryPort.getByIdOrThrow(savedCategory.id)

        assertThat(foundCategory.id).isEqualTo(savedCategory.id)
        assertThat(foundCategory.name).isEqualTo("테스트 카테고리")
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 ID 조회 시 404-2")
    fun t2() {
        val exception = assertThrows<ServiceException> {
            categoryPort.getByIdOrThrow(Int.MAX_VALUE)
        }

        assertThat(exception.resultCode).isEqualTo("404-2")
        assertThat(exception.msg).isEqualTo("존재하지 않는 카테고리입니다.")
    }
}
