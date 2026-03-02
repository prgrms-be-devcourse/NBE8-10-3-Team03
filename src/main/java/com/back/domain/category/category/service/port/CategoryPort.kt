package com.back.domain.category.category.service.port

import com.back.domain.category.category.entity.Category

// 카테고리 영속 접근을 추상화한 포트.
// 다른 도메인은 이 인터페이스에만 의존하고, 구현(Repository/JPA)은 어댑터로 숨긴다.
interface CategoryPort {
    fun getByIdOrThrow(categoryId: Int): Category
    fun findByNameOrNull(name: String): Category?
    fun count(): Long
    fun save(category: Category): Category
    fun findAll(): List<Category>
}
