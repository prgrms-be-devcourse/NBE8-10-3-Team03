package com.back.domain.category.category.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CategoryTest {
    @Test
    @DisplayName("카테고리 생성 시 이름 앞뒤 공백은 trim 된다.")
    fun t1() {
        val category = Category("  전자기기  ")

        assertThat(category.name).isEqualTo("전자기기")
    }

    @Test
    @DisplayName("카테고리 이름이 비어 있으면 예외가 발생한다.")
    fun t2() {
        val exception = assertThrows<IllegalArgumentException> {
            Category("   ")
        }

        assertThat(exception.message).isEqualTo("카테고리 이름은 비어 있을 수 없습니다.")
    }
}
