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
import kotlin.jvm.JvmName

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @field:Id
    @field:GeneratedValue(strategy = IDENTITY)
    @get:JvmName("getIdProperty")
    @set:JvmName("setIdProperty")
    var id: Int = 0
        protected set

    @CreatedDate
    lateinit var createDate: LocalDateTime
        protected set

    @LastModifiedDate
    lateinit var modifyDate: LocalDateTime
        protected set

    fun getId(): Int = id

    protected fun setId(id: Int) {
        this.id = id
    }
}
