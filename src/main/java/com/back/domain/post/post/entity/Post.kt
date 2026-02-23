package com.back.domain.post.post.entity

import com.back.domain.category.category.entity.Category
import com.back.domain.member.member.entity.Member
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
class Post(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    val seller: Member,

    @Column(length = 50, nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    var price: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category,

    @Enumerated(EnumType.STRING)
    var status: PostStatus = PostStatus.SALE,

    var deleted: Boolean = false

) : BaseEntity() {

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    var postImages: MutableList<PostImage> = mutableListOf()
        protected set // 외부에서 리스트 자체를 통째로 교체하지 못하게 보호

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    var viewCount: Long = 0L
        protected set

    fun addPostImage(postImage: PostImage) {
        this.postImages.add(postImage)
    }

    fun update(title: String, content: String, price: Int, category: Category) {
        this.title = title
        this.content = content
        this.price = price
        this.category = category
    }

    fun increaseViewCount() {
        this.viewCount++
    }

    fun updateStatus(status: PostStatus) {
        this.status = status
    }
}