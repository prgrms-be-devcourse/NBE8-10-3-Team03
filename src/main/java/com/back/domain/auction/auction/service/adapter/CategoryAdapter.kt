package com.back.domain.auction.auction.service.adapter

import com.back.domain.auction.auction.service.port.CategoryPort
import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.repository.CategoryRepository
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Component

// 카테고리 조회 인프라를 포트로 감싸 서비스가 CategoryRepository를 직접 참조하지 않게 한다.
@Component
class CategoryAdapter(
    private val categoryRepository: CategoryRepository
) : CategoryPort {
    override fun getByIdOrThrow(categoryId: Int): Category =
        categoryRepository.findById(categoryId)
            .orElseThrow { ServiceException("404-2", "존재하지 않는 카테고리입니다.") }
}
