package com.back.domain.category.category.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "categories")
class Category protected constructor() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0
        private set

    @Column(nullable = false, length = 100)
    lateinit var name: String
        private set

    constructor(name: String) : this() {
        this.name = name
    }
}
