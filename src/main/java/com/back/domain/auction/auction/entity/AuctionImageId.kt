package com.back.domain.auction.auction.entity

import java.io.Serializable

data class AuctionImageId(
    var auction: Int? = null,
    var image: Int? = null
) : Serializable
