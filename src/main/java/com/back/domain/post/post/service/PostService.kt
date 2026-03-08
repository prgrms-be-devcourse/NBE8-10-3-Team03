package com.back.domain.post.post.service

import com.back.domain.auction.auction.dto.response.AuctionPageResponse
import com.back.domain.auction.auction.entity.AuctionStatus
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
import com.back.domain.post.post.service.port.PostPort
import com.back.domain.post.post.service.port.PostUseCase
import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import com.back.global.storage.port.FileStoragePort
import com.back.global.util.PageUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.ser.PropertyWriter

@Service
@Transactional(readOnly = true)
class PostService(
    private val postPort: PostPort,
    private val categoryPort: CategoryPort,
    private val imageRepository: ImageRepository,
    private val fileStoragePort: FileStoragePort,
    private val memberService: MemberService,
    private val postCountService: PostCountService
) : PostUseCase {

    @Transactional
    override fun create(actor: Member, req: PostCreateRequest): Int {
        // 기존 Java의 Optional.get() 호출 방식을 Kotlin 람다로 깔끔하게 변환
        val member = memberService.findById(actor.id)
            ?: throw  ServiceException("404-3", "회원 정보를 찾을 수 없습니다.")

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

        postPort.save(post)

        var thumbnailSet = false

        // null-safe 연산자(?.)와 forEach를 결합한 간결한 반복문
        req.images?.forEach { file ->
            if (!file.isEmpty) {
                try {
                    val imageUrl = fileStoragePort.storeFile(file, "post")
                    val image = imageRepository.save(Image(imageUrl))
                    post.addPostImage(PostImage(post, image))

                    // 첫 번째 업로드 이미지일 경우만 썸네일 지정
                    if (!thumbnailSet) {
                        post.thumbnailUrl = imageUrl
                        thumbnailSet = true
                    }
                } catch (e: Exception) {
                    throw ServiceException("500-1", "이미지 저장에 실패했습니다: ${e.message}")
                }
            }
        }
        return post.id
    }

    @Transactional
    override fun modify(actor: Member, id: Int, req: PostUpdateRequest): Int {
        // Kotlin에서는 Post? 타입을 반환하므로 ?: 연산자(Elvis)로 예외 처리 가능
        val post = postPort.findByIdAndDeletedFalse(id)
            ?: throw ServiceException("404-1", "존재하지 않는 글입니다.")

        if (post.seller.id != actor.id) {
            throw ServiceException("403-1", "자신의 글만 수정할 수 있습니다.")
        }

        val member = memberService.findById(actor.id)
            ?: throw  ServiceException("404-3", "회원 정보를 찾을 수 없습니다.")
        if (member.status == MemberStatus.SUSPENDED) {
            throw ServiceException("403-3", "정지된 회원은 해당 기능을 사용할 수 없습니다.")
        }

        val categoryId = req.categoryId ?: throw ServiceException("400", "카테고리 ID가 누락되었습니다.")
        // 카테고리 조회 실패 메시지/코드 정책은 CategoryPort 구현체에서 일관되게 관리한다.
        val category = categoryPort.getByIdOrThrow(categoryId)

        post.update(req.title, req.content, req.price, category)
        updateImages(req, post)
        return post.id
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
                val url = fileStoragePort.storeFile(file, "post")
                val img = imageRepository.save(Image(url))
                post.addPostImage(PostImage(post, img))
            }
        }
    }

    @Transactional
    override fun delete(actor: Member, id: Int) {
        val post = postPort.findByIdAndDeletedFalse(id)
            ?: throw ServiceException("404-1", "존재하지 않는 글입니다.")

        if (post.seller.id != actor.id) {
            throw ServiceException("403-1", "자신의 글만 삭제할 수 있습니다.")
        }
        post.deleted = true
    }

    @Transactional
    override fun getDetail(id: Int): PostDetailResponse {
        val post = postPort.findByIdAndDeletedFalse(id)
            ?: throw ServiceException("404-1", "존재하지 않는 글입니다.")

        post.increaseViewCount()
        return PostDetailResponse(post)
    }

    fun getList(pageable: Pageable): Page<PostListResponse> {
        return postPort.findAllByDeletedFalse(pageable)
            .map { PostListResponse(it) } // map 내부에서 람다 it 사용
    }

    @Transactional
    override fun updatePostStatus(actor: Member, id: Int, status: PostStatus) {
        val post = postPort.findByIdAndDeletedFalse(id)
            ?: throw ServiceException("404-1", "존재하지 않는 글입니다.")

        if (post.seller.id != actor.id) {
            throw ServiceException("403-1", "자신의 글만 상태를 수정할 수 있습니다.")
        }

        post.updateStatus(status)
    }

    // ===== 목록 조회 ======
    override fun getList(
        page: Int,
        size: Int,
        sortBy: String?,
        categoryName: String?,
        statusStr: String?)
            : PostPageResponse {
        val pageable = PageUtils.createPageable(page, size, createSort(sortBy))

        val status = if (!statusStr.isNullOrEmpty() && statusStr.lowercase() != "all") {
            PostStatus.valueOf(statusStr.uppercase())
        } else {
            null
        }

        val category = categoryName?.takeIf { it.isNotBlank() }?.trim()
            ?.let { normalizedName ->
                categoryPort.findByNameOrNull(normalizedName)
                    ?: return PostPageResponse.of(emptyList(), page, size, 0)
            }

        val categoryId = category?.id

        val postSlice = when {
            categoryId != null && status != null ->
                postPort.findSliceProjectionByCategoryIdAndStatus(
                    categoryId,
                    status,
                    pageable
                )

            categoryId != null ->
                postPort.findSliceProjectionByCategoryId(
                    categoryId,
                    pageable
                )

            status != null ->
                postPort.findSliceProjectionByStatus(status, pageable)
            else ->
                postPort.findSliceProjectionAll(pageable)
        }

        val countCacheKey = buildCountCacheKey(categoryId, status)
        val totalElements = resolveTotalElements(page, size, postSlice, countCacheKey, categoryId, status)
        val content = postSlice.content.map(PostListResponse::from)

        return PostPageResponse.of(content, page, size, totalElements)
    }

    private fun resolveTotalElements(
        page: Int,
        size: Int,
        postSlice: Slice<*>,
        countCacheKey: String,
        categoryId: Int?,
        status: PostStatus?
    ): Long {
        val currentElements = postSlice.numberOfElements.toLong()

        // 첫 페이지에서 size 미만이면 남은 페이지가 없으므로 count 쿼리를 생략할 수 있다.
        if (page == 0 && currentElements < size.toLong()) return currentElements

        // 마지막 페이지면 현재 페이지 오프셋으로 전체 건수를 정확히 계산할 수 있다.
        if (!postSlice.hasNext()) return page.toLong() * size + currentElements

        return postCountService.getTotalCount(countCacheKey, categoryId, status)
    }

    private fun buildCountCacheKey(categoryId: Int?, status: PostStatus?): String =
        "category:${categoryId ?: "ALL"}:status:${status?.name ?: "ALL"}"

    private fun createSort(sortBy: String?): Sort {
        if (sortBy.isNullOrBlank()) {
            return Sort.by(
                Sort.Order.desc("createDate"),
                Sort.Order.desc("id")
            )
        }

        val sortParams = sortBy.split(",")
        val property = sortParams[0]
        val direction = if (sortParams.size > 1 && sortParams[1].equals("asc", ignoreCase = true)) {
            Sort.Direction.ASC
        } else {
            Sort.Direction.DESC
        }
        return if (property == "createDate") {
            Sort.by(Sort.Order(direction, "createDate"), Sort.Order(direction, "id"))
        } else {
            Sort.by(direction, property)
        }
    }

    override fun getListByUserId(pageable: Pageable, userId: Int, statusStr: String?): PostPageResponse {
        val status = if (!statusStr.isNullOrEmpty() && statusStr.lowercase() != "all") {
            PostStatus.valueOf(statusStr.uppercase())
        } else {
            null
        }

        val postPage = if (status == null) {
            postPort.findBySellerId(userId, pageable)
        } else {
            postPort.findBySellerIdAndStatus(userId, status, pageable)
        }

        val dtoList = postPage.content.map { PostListResponse(it) }
        return PostPageResponse(
            content = dtoList,
            page = postPage.number,
            size = postPage.size,
            totalElements = postPage.totalElements,
            totalPages = postPage.totalPages
        )
    }
}
