package com.back.global.util

import com.back.global.exception.ServiceException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

object PageUtils {
    // 페이징 기본값 상수
    const val DEFAULT_PAGE: Int = 0
    const val DEFAULT_SIZE: Int = 20
    const val MAX_SIZE: Int = 100

    /**
     * 페이징 파라미터 검증 및 Pageable 생성
     *
     * @param page   페이지 번호 (0부터 시작)
     * @param size   페이지 크기
     * @param sort   정렬 조건
     * @return Pageable 객체
     * @throws ServiceException 유효하지 않은 페이징 파라미터
     */
    fun createPageable(page: Int, size: Int, sort: Sort): Pageable {
        validatePageParameters(page, size)
        return PageRequest.of(page, size, sort)
    }

    /**
     * 페이징 파라미터 검증 (정렬 없이)
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return Pageable 객체
     */
    fun createPageable(page: Int, size: Int): Pageable {
        validatePageParameters(page, size)
        return PageRequest.of(page, size)
    }

    /**
     * 페이징 파라미터 유효성 검증
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @throws ServiceException 유효하지 않은 파라미터
     */
    fun validatePageParameters(page: Int, size: Int) {
        when {
            page < 0 -> throw ServiceException("400-1", "페이지 번호는 0 이상이어야 합니다.")
            page <= 0 -> throw ServiceException("400-1", "페이지 크기는 1 이상이어야 합니다.")
            size > MAX_SIZE -> throw ServiceException("400-1", String.format("페이지 크기는 %d 이하여야 합니다.", MAX_SIZE))
        }
    }
}

