package com.back.domain.post.post.service

import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.service.port.CategoryPort
import com.back.domain.image.image.entity.Image
import com.back.domain.image.image.repository.ImageRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.enums.MemberStatus
import com.back.domain.member.member.enums.Role
import com.back.domain.member.member.service.MemberService
import com.back.domain.post.post.dto.PostCreateRequest
import com.back.domain.post.post.dto.PostUpdateRequest
import com.back.domain.post.post.entity.Post
import com.back.domain.post.post.entity.PostImage
import com.back.domain.post.post.entity.PostStatus
import com.back.domain.post.post.repository.PostRepository
import com.back.global.exception.ServiceException
import com.back.global.storage.port.FileStoragePort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.Optional

class PostServiceTest {
    private val postRepository: PostRepository = mock(PostRepository::class.java) { invocation ->
        if (invocation.method.name == "save") {
            invocation.arguments[0]
        } else {
            Answers.RETURNS_DEFAULTS.answer(invocation)
        }
    }
    private val categoryPort: CategoryPort = mock(CategoryPort::class.java)
    private val imageRepository: ImageRepository = mock(ImageRepository::class.java)
    private val fileStoragePort: FileStoragePort = mock(FileStoragePort::class.java)
    private val memberService: MemberService = mock(MemberService::class.java)

    private val postService = PostService(
        postRepository,
        categoryPort,
        imageRepository,
        fileStoragePort,
        memberService
    )

    @Test
    @DisplayName("게시글 생성 성공 시 게시글 ID를 반환한다.")
    fun t1() {
        val actor = member(1, "seller")
        val category = Category("전자기기")
        val req = PostCreateRequest(
            title = "아이폰 판매",
            content = "상태 좋습니다. 네고 불가.",
            price = 100000,
            categoryId = 10,
            images = null
        )

        `when`(memberService.findById(1)).thenReturn(Optional.of(actor))
        `when`(categoryPort.getByIdOrThrow(10)).thenReturn(category)

        val postId = postService.create(actor, req)

        assertThat(postId).isZero()
    }

    @Test
    @DisplayName("정지 회원은 게시글 생성이 불가하다.")
    fun t2() {
        val actor = member(1, "seller").apply { status = MemberStatus.SUSPENDED }
        val req = PostCreateRequest(
            title = "제목",
            content = "10자 이상 내용입니다.",
            price = 1000,
            categoryId = 1
        )
        `when`(memberService.findById(1)).thenReturn(Optional.of(actor))

        assertThatThrownBy { postService.create(actor, req) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("403-3")
    }

    @Test
    @DisplayName("카테고리 ID 누락 시 게시글 생성은 실패한다.")
    fun t3() {
        val actor = member(1, "seller")
        val req = PostCreateRequest(
            title = "제목",
            content = "10자 이상 내용입니다.",
            price = 1000,
            categoryId = null
        )
        `when`(memberService.findById(1)).thenReturn(Optional.of(actor))

        assertThatThrownBy { postService.create(actor, req) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400")
    }

    @Test
    @DisplayName("이미지 저장 실패 시 500 예외를 반환한다.")
    fun t4() {
        val actor = member(1, "seller")
        val category = Category("전자기기")
        val imageFile = MockMultipartFile("images", "a.jpg", "image/jpeg", "bytes".toByteArray())
        val req = PostCreateRequest(
            title = "제목",
            content = "10자 이상 내용입니다.",
            price = 1000,
            categoryId = 1,
            images = listOf(imageFile)
        )

        `when`(memberService.findById(1)).thenReturn(Optional.of(actor))
        `when`(categoryPort.getByIdOrThrow(1)).thenReturn(category)
        `when`(fileStoragePort.storeFile(imageFile, "post")).thenThrow(RuntimeException("S3 down"))

        assertThatThrownBy { postService.create(actor, req) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("500-1")
    }

    @Test
    @DisplayName("게시글 수정 시 대상 글이 없으면 404 예외다.")
    fun t5() {
        val actor = member(1, "seller")
        val req = PostUpdateRequest("제목", "내용", 1000, 1)
        `when`(postRepository.findByIdAndDeletedFalse(999)).thenReturn(null)

        assertThatThrownBy { postService.modify(actor, 999, req) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("404-1")
    }

    @Test
    @DisplayName("게시글 수정은 작성자만 가능하다.")
    fun t6() {
        val owner = member(1, "owner")
        val actor = member(2, "other")
        val post = post(id = 10, seller = owner)
        val req = PostUpdateRequest("제목", "내용", 1000, 1)
        `when`(postRepository.findByIdAndDeletedFalse(10)).thenReturn(post)

        assertThatThrownBy { postService.modify(actor, 10, req) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("403-1")
    }

    @Test
    @DisplayName("수정 시 keepImageUrls가 비어 있으면 기존 이미지를 모두 제거한다.")
    fun t7() {
        val actor = member(1, "seller")
        val category = Category("전자기기")
        val post = post(id = 11, seller = actor, category = category)
        val existingImage = Image("https://img.old/1.jpg")
        post.addPostImage(PostImage(post, existingImage))
        val req = PostUpdateRequest(
            title = "수정 제목",
            content = "수정 내용",
            price = 2000,
            categoryId = 2,
            images = null,
            keepImageUrls = emptyList()
        )

        `when`(postRepository.findByIdAndDeletedFalse(11)).thenReturn(post)
        `when`(memberService.findById(1)).thenReturn(Optional.of(actor))
        `when`(categoryPort.getByIdOrThrow(2)).thenReturn(Category("가구"))

        postService.modify(actor, 11, req)

        assertThat(post.postImages).isEmpty()
        assertThat(post.title).isEqualTo("수정 제목")
    }

    @Test
    @DisplayName("게시글 삭제 성공 시 deleted=true 처리된다.")
    fun t8() {
        val actor = member(1, "seller")
        val post = post(id = 12, seller = actor)
        `when`(postRepository.findByIdAndDeletedFalse(12)).thenReturn(post)

        postService.delete(actor, 12)

        assertThat(post.deleted).isTrue()
    }

    @Test
    @DisplayName("게시글 상세 조회 시 조회수가 증가한다.")
    fun t9() {
        val actor = member(1, "seller")
        val post = post(id = 13, seller = actor)
        ReflectionTestUtils.setField(post, "createDate", LocalDateTime.now().minusDays(1))
        `when`(postRepository.findByIdAndDeletedFalse(13)).thenReturn(post)

        val detail = postService.getDetail(13)

        assertThat(detail.id).isEqualTo(13)
        assertThat(post.viewCount).isEqualTo(1)
    }

    @Test
    @DisplayName("게시글 상태 변경은 작성자만 가능하다.")
    fun t10() {
        val owner = member(1, "owner")
        val actor = member(2, "other")
        val post = post(id = 14, seller = owner)
        `when`(postRepository.findByIdAndDeletedFalse(14)).thenReturn(post)

        assertThatThrownBy { postService.updatePostStatus(actor, 14, PostStatus.SOLD) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("403-1")
    }

    private fun member(id: Int, username: String): Member {
        val member = Member(username, "pw", "$username-nick", Role.USER, null)
        ReflectionTestUtils.setField(member, "id", id)
        return member
    }

    private fun post(id: Int, seller: Member, category: Category = Category("전자기기")): Post {
        return Post(
            seller = seller,
            title = "제목",
            content = "충분히 긴 내용입니다.",
            price = 1000,
            category = category,
            status = PostStatus.SALE
        ).also {
            ReflectionTestUtils.setField(it, "id", id)
            ReflectionTestUtils.setField(it, "createDate", LocalDateTime.now().minusHours(2))
        }
    }
}
