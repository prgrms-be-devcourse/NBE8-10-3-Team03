package com.back.domain.image.image.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "images")
/*
class Image protected constructor() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0
        private set

    @Column(nullable = false, length = 500)
    lateinit var url: String
        private set

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: LocalDateTime
        private set

    constructor(url: String) : this() {
        this.url = url
        this.createdAt = LocalDateTime.now()
    }
 */
class Image(

    @Column(nullable = false, length = 500)
    var url: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
){
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0
}
