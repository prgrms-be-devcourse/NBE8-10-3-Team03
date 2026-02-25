package com.back.domain.auction.auction.service.port

import com.back.domain.category.category.entity.Category

interface CategoryPort {
    fun getByIdOrThrow(categoryId: Int): Category
}
