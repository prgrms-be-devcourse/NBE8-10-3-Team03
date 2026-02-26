package com.back.global.jpa.entity

import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @field:Id
    @field:GeneratedValue(strategy = IDENTITY)
    var id: Int = 0
        protected set

    @CreatedDate
    lateinit var createDate: LocalDateTime
        protected set

    @LastModifiedDate
    lateinit var modifyDate: LocalDateTime
        protected set
}
