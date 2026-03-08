package com.back.domain.post.post.dto

import com.back.domain.auction.auction.dto.response.AuctionListItemDto
import com.back.domain.auction.auction.dto.response.AuctionPageResponse
import org.springframework.data.domain.Page
import java.awt.print.Pageable

data class PostPageResponse(
    val content: List<PostListResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
//    val currentStatusFilter: String
) {
    companion object {
        @JvmStatic
        fun from(page: Page<PostListResponse>): PostPageResponse =
            PostPageResponse(
                content = page.content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages
            )

        @JvmStatic
        fun of(
            content: List<PostListResponse>,
            page: Int,
            size: Int,
            totalElements: Long
        ): PostPageResponse {
            val totalPages = if (totalElements == 0L) 0 else ((totalElements + size - 1) / size).toInt()
            return PostPageResponse(
                content = content,
                page = page,
                size = size,
                totalElements = totalElements,
                totalPages = totalPages
            )
        }
    }
}