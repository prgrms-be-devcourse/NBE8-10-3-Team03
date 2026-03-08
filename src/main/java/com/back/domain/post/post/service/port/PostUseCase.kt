package com.back.domain.post.post.service.port

import com.back.domain.post.post.dto.PostCreateRequest
import com.back.domain.post.post.dto.PostDetailResponse
import com.back.domain.post.post.dto.PostPageResponse
import com.back.domain.post.post.dto.PostUpdateRequest
import com.back.domain.post.post.entity.PostStatus
import com.back.domain.member.member.entity.Member
import org.springframework.data.domain.Pageable

interface PostUseCase {
    fun create(actor: Member, req: PostCreateRequest): Int
    fun modify(actor: Member, id: Int, req: PostUpdateRequest): Int
    fun delete(actor: Member, id: Int)
    fun getDetail(id: Int): PostDetailResponse
    fun getList(page: Int, size: Int, sortBy: String?, categoryName: String?, statusStr: String?): PostPageResponse
    fun getListByUserId(pageable: Pageable, userId: Int, statusStr: String?): PostPageResponse
    fun updatePostStatus(actor: Member, id: Int, status: PostStatus)
}