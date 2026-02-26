package com.back.domain.image.image.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ImageTest {
    @Test
    @DisplayName("이미지 생성 시 URL 과 createdAt 이 초기화된다.")
    fun t1() {
        val before = LocalDateTime.now().minusSeconds(1)
        val image = Image("/uploads/test-image.jpg")
        val after = LocalDateTime.now().plusSeconds(1)

        assertThat(image.url).isEqualTo("/uploads/test-image.jpg")
        assertThat(image.createdAt).isAfter(before)
        assertThat(image.createdAt).isBefore(after)
    }
}
