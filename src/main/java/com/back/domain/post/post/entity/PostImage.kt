package com.back.domain.post.post.entity

import com.back.domain.image.image.entity.Image
import jakarta.persistence.*

@Entity
@Table(name = "post_images")
@IdClass(PostImageId::class)
class PostImage(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: Post,

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    val image: Image
)