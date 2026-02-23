package com.back.domain.auction.auction.entity

enum class CancellerRole(
    val description: String
) {
    SELLER("판매자"),
    BUYER("구매자")
}
