package com.back.domain.bid.bid.entity

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.member.member.entity.Member
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "bids")
class Bid protected constructor() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    lateinit var auction: Auction
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    lateinit var bidder: Member
        private set

    @Column(nullable = false)
    var price: Int = 0
        private set

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: LocalDateTime
        private set

    constructor(auction: Auction, bidder: Member, price: Int) : this() {
        this.auction = auction
        this.bidder = bidder
        this.price = price
        this.createdAt = LocalDateTime.now()
    }
}
