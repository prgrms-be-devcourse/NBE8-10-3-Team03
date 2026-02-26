package com.back.domain.category.category.repository

import com.back.domain.category.category.entity.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Int>
