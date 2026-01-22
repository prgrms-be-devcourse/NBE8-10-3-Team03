package com.back.global.util;

import com.back.global.exception.ServiceException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageUtils {

    private PageUtils() {
        // Utility class
    }

    /**
     * 페이징 파라미터 검증 및 Pageable 생성
     *
     * @param page   페이지 번호 (0부터 시작)
     * @param size   페이지 크기
     * @param sort   정렬 조건
     * @return Pageable 객체
     * @throws ServiceException 유효하지 않은 페이징 파라미터
     */
    public static Pageable createPageable(int page, int size, Sort sort) {
        validatePageParameters(page, size);
        return PageRequest.of(page, size, sort);
    }

    /**
     * 페이징 파라미터 검증 (정렬 없이)
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return Pageable 객체
     */
    public static Pageable createPageable(int page, int size) {
        validatePageParameters(page, size);
        return PageRequest.of(page, size);
    }

    /**
     * 페이징 파라미터 유효성 검증
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @throws ServiceException 유효하지 않은 파라미터
     */
    public static void validatePageParameters(int page, int size) {
        if (page < 0) {
            throw new ServiceException("400-1", "페이지 번호는 0 이상이어야 합니다.");
        }
        if (size <= 0) {
            throw new ServiceException("400-1", "페이지 크기는 1 이상이어야 합니다.");
        }
    }
}

