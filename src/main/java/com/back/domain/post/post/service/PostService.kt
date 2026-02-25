package com.back.domain.post.post.service

import com.back.domain.auction.auction.service.FileStorageService
import com.back.domain.category.category.service.port.CategoryPort
import com.back.domain.image.image.entity.Image
import com.back.domain.image.image.repository.ImageRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.enums.MemberStatus
import com.back.domain.member.member.service.MemberService
import com.back.domain.post.post.dto.*
import com.back.domain.post.post.entity.Post
import com.back.domain.post.post.entity.PostImage
import com.back.domain.post.post.entity.PostStatus
import com.back.domain.post.post.repository.PostRepository
import com.back.global.exception.ServiceException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val categoryPort: CategoryPort,
    private val imageRepository: ImageRepository,
    private val fileStorageService: FileStorageService,
    private val memberService: MemberService
) {

    @Transactional
    fun create(actor: Member, req: PostCreateRequest): Int {
        // 기존 Java의 Optional.get() 호출 방식을 Kotlin 람다로 깔끔하게 변환
        val member = memberService.findById(actor.id as Int)
            .orElseThrow { ServiceException("404-3", "회원 정보를 찾을 수 없습니다.") }

        if (member.status == MemberStatus.SUSPENDED) {
            throw ServiceException("403-3", "정지된 회원은 해당 기능을 사용할 수 없습니다.")
        }

        val categoryId = req.categoryId ?: throw ServiceException("400", "카테고리 ID가 누락되었습니다.")
        // Post 도메인도 CategoryRepository 대신 포트에 의존해 DIP를 유지한다.
        val category = categoryPort.getByIdOrThrow(categoryId)

        val post = Post(
            seller = actor,
            title = req.title,
            content = req.content,
            price = req.price,
            category = category,
            status = PostStatus.SALE
        )

        postRepository.save(post)

        // null-safe 연산자(?.)와 forEach를 결합한 간결한 반복문
        req.images?.forEach { file ->
            if (!file.isEmpty) {
                try {
                    val imageUrl = fileStorageService.storeFile(file)
                    val image = imageRepository.save(Image(imageUrl))
                    post.addPostImage(PostImage(post, image))
                } catch (e: Exception) {
                    throw ServiceException("500-1", "이미지 저장에 실패했습니다: ${e.message}")
                }
            }
        }
        return post.id as Int
    }

    @Transactional
    fun modify(actor: Member, id: Int, req: PostUpdateRequest) {
        // Kotlin에서는 Post? 타입을 반환하므로 ?: 연산자(Elvis)로 예외 처리 가능
        val post = postRepository.findByIdAndDeletedFalse(id)
            ?: throw ServiceException("404-1", "존재하지 않는 글입니다.")

        if (post.seller.id != actor.id) {
            throw ServiceException("403-1", "자신의 글만 수정할 수 있습니다.")
        }

        val member = memberService.findById(actor.id as Int)
            .orElseThrow { ServiceException("404-3", "회원 정보를 찾을 수 없습니다.") }
        if (member.status == MemberStatus.SUSPENDED) {
            throw ServiceException("403-3", "정지된 회원은 해당 기능을 사용할 수 없습니다.")
        }

        val categoryId = req.categoryId ?: throw ServiceException("400", "카테고리 ID가 누락되었습니다.")
        // 카테고리 조회 실패 메시지/코드 정책은 CategoryPort 구현체에서 일관되게 관리한다.
        val category = categoryPort.getByIdOrThrow(categoryId)

        post.update(req.title, req.content, req.price, category)
        updateImages(req, post)
    }

    private fun updateImages(req: PostUpdateRequest, post: Post) {
        val keepUrls = req.keepImageUrls
        if (keepUrls.isNullOrEmpty()) {
            post.postImages.clear()
        } else {
            post.postImages.removeIf { !keepUrls.contains(it.image.url) }
        }

        req.images?.forEach { file ->
            if (!file.isEmpty) {
                val url = fileStorageService.storeFile(file)
                val img = imageRepository.save(Image(url))
                post.addPostImage(PostImage(post, img))
            }
        }
    }

    @Transactional
    fun delete(actor: Member, id: Int) {
        val post = postRepository.findByIdAndDeletedFalse(id)
            ?: throw ServiceException("404-1", "존재하지 않는 글입니다.")

        if (post.seller.id != actor.id) {
            throw ServiceException("403-1", "자신의 글만 삭제할 수 있습니다.")
        }
        post.deleted = true
    }

    @Transactional
    fun getDetail(id: Int): PostDetailResponse {
        val post = postRepository.findByIdAndDeletedFalse(id)
            ?: throw ServiceException("404-1", "존재하지 않는 글입니다.")

        post.increaseViewCount()
        return PostDetailResponse(post)
    }

    fun getList(pageable: Pageable): Page<PostListResponse> {
        return postRepository.findAllByDeletedFalse(pageable)
            .map { PostListResponse(it) } // map 내부에서 람다 it 사용
    }

    @Transactional
    fun updatePostStatus(actor: Member, id: Int, status: PostStatus) {
        val post = postRepository.findByIdAndDeletedFalse(id)
            ?: throw ServiceException("404-1", "존재하지 않는 글입니다.")

        if (post.seller.id != actor.id) {
            throw ServiceException("403-1", "자신의 글만 상태를 수정할 수 있습니다.")
        }

        post.updateStatus(status)
    }

    fun getList(pageable: Pageable, statusStr: String?): PostPageResponse {
        val status = if (!statusStr.isNullOrEmpty() && statusStr.lowercase() != "all") {
            PostStatus.valueOf(statusStr.uppercase())
        } else {
            null
        }

        val postPage = postRepository.findPostsByStatus(status, pageable)

        val dtoList = postPage.content.map { PostListResponse(it) }

        return PostPageResponse(
            content = dtoList,
            page = postPage.number,
            size = postPage.size,
            totalElements = postPage.totalElements,
            totalPages = postPage.totalPages,
            currentStatusFilter = statusStr ?: "all"
        )
    }

    fun getListByUserId(pageable: Pageable, userId: Int, statusStr: String?): PostPageResponse {
        val status = if (!statusStr.isNullOrEmpty() && statusStr.lowercase() != "all") {
            PostStatus.valueOf(statusStr.uppercase())
        } else {
            null
        }

        val postPage = if (status == null) {
            postRepository.findBySellerId(userId, pageable)
        } else {
            postRepository.findBySellerIdAndStatus(userId, status, pageable)
        }

        val dtoList = postPage.content.map { PostListResponse(it) }

        return PostPageResponse(
            content = dtoList,
            page = postPage.number,
            size = postPage.size,
            totalElements = postPage.totalElements,
            totalPages = postPage.totalPages,
            currentStatusFilter = statusStr ?: "all"
        )
    }
}
