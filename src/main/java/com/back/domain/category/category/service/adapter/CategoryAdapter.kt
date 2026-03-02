package com.back.domain.category.category.service.adapter

import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.repository.CategoryRepository
import com.back.domain.category.category.service.port.CategoryPort
import com.back.global.exception.ServiceException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

// CategoryPort의 JPA 구현체.
// 도메인/서비스 계층이 CategoryRepository를 직접 참조하지 않도록 의존 방향을 고정한다.
@Component
class CategoryAdapter(
    private val categoryRepository: CategoryRepository
) : CategoryPort {
    override fun getByIdOrThrow(categoryId: Int): Category =
        categoryRepository.findByIdOrNull(categoryId)
            ?: throw ServiceException("404-2", "존재하지 않는 카테고리입니다.")

    override fun findByNameOrNull(name: String): Category? = categoryRepository.findByName(name)

    override fun count(): Long = categoryRepository.count()

    override fun save(category: Category): Category = categoryRepository.save(category)

    override fun findAll(): List<Category> = categoryRepository.findAll()
}
