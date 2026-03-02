package com.back.domain.auction.auction.entity

import com.back.domain.category.category.entity.Category
import com.back.domain.member.member.entity.Member
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.time.LocalDateTime

@Entity
class Auction protected constructor() : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    lateinit var seller: Member
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    lateinit var category: Category
        private set

    @Column(nullable = false)
    lateinit var name: String
        private set

    @Column(columnDefinition = "TEXT")
    var description: String? = null
        private set

    @Column(name = "start_price", nullable = false)
    var startPrice: Int? = null
        private set

    @Column(name = "buy_now_price")
    var buyNowPrice: Int? = null
        private set

    @Column(name = "current_highest_bid")
    var currentHighestBid: Int? = null
        private set

    @Column(name = "bid_count", nullable = false)
    var bidCount: Int = 0
        private set

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: AuctionStatus = AuctionStatus.OPEN
        private set

    @Column(name = "start_at", nullable = false)
    lateinit var startAt: LocalDateTime
        private set

    @Column(name = "end_at", nullable = false)
    lateinit var endAt: LocalDateTime
        private set

    @Column(name = "winner_id")
    var winnerId: Int? = null
        private set

    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = null
        private set

    @Column(name = "cancelled_by")
    var cancelledBy: Int? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "canceller_role", length = 20)
    var cancellerRole: CancellerRole? = null
        private set

    @OneToMany(mappedBy = "auction", cascade = [CascadeType.ALL], orphanRemoval = true)
    var auctionImages: MutableList<AuctionImage> = mutableListOf()
        private set

    var thumbnailUrl: String? = null

    fun addAuctionImage(auctionImage: AuctionImage) {
        auctionImages.add(auctionImage)
        auctionImage.auction = this
    }

    fun isSeller(memberId: Int?): Boolean = seller.id == memberId

    fun hasBids(): Boolean = bidCount > 0

    fun updateBeforeBid(
        name: String?,
        description: String?,
        startPrice: Int?,
        buyNowPrice: Int?,
        endAt: LocalDateTime?
    ) {
        if (!name.isNullOrBlank()) this.name = name
        if (description != null) this.description = description
        if (startPrice != null) this.startPrice = startPrice
        if (buyNowPrice != null) this.buyNowPrice = buyNowPrice
        if (endAt != null) this.endAt = endAt
    }

    fun updateAfterBid(name: String?, description: String?) {
        if (!name.isNullOrBlank()) this.name = name
        if (description != null) this.description = description
    }

    fun removeAuctionImage(auctionImage: AuctionImage) = auctionImages.remove(auctionImage)

    fun clearAuctionImages() = auctionImages.clear()

    fun updateBid(newPrice: Int?) {
        currentHighestBid = newPrice
        bidCount++
    }

    fun closeAuction() {
        status = AuctionStatus.COMPLETED
        closedAt = LocalDateTime.now()
    }

    fun completeWithWinner(winnerId: Int?) {
        this.winnerId = winnerId
        status = AuctionStatus.COMPLETED
        closedAt = LocalDateTime.now()
    }

    fun closeWithoutBid() {
        status = AuctionStatus.CLOSED
        closedAt = LocalDateTime.now()
    }

    fun cancelTrade(userId: Int?, role: CancellerRole?) {
        status = AuctionStatus.CANCELLED
        cancelledBy = userId
        cancellerRole = role
        closedAt = LocalDateTime.now()
    }

    fun determineCancellerRole(memberId: Int?): CancellerRole {
        // 거래 취소는 낙찰 완료(COMPLETED) 이후에만 허용한다.
        if (status != AuctionStatus.COMPLETED) {
            throw IllegalStateException("낙찰 완료된 경매만 취소할 수 있습니다.")
        }

        if (seller.id == memberId) return CancellerRole.SELLER
        if (winnerId != null && winnerId == memberId) return CancellerRole.BUYER

        throw IllegalArgumentException("거래를 취소할 권한이 없습니다.")
    }

    fun canCancelTrade(memberId: Int?): Boolean =
        status == AuctionStatus.COMPLETED && (seller.id == memberId || winnerId == memberId)

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(endAt)

    fun isActive(): Boolean = status == AuctionStatus.OPEN && !isExpired()

    fun isCompleted(): Boolean = status == AuctionStatus.COMPLETED

    fun isClosed(): Boolean =
        status == AuctionStatus.CLOSED || status == AuctionStatus.COMPLETED || status == AuctionStatus.CANCELLED

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var seller: Member? = null
        private var category: Category? = null
        private var name: String? = null
        private var description: String? = null
        private var startPrice: Int? = null
        private var buyNowPrice: Int? = null
        private var startAt: LocalDateTime? = null
        private var endAt: LocalDateTime? = null

        fun seller(seller: Member) = apply { this.seller = seller }
        fun category(category: Category) = apply { this.category = category }
        fun name(name: String) = apply { this.name = name }
        fun description(description: String) = apply { this.description = description }
        fun startPrice(startPrice: Int) = apply { this.startPrice = startPrice }
        fun buyNowPrice(buyNowPrice: Int?) = apply { this.buyNowPrice = buyNowPrice }
        fun startAt(startAt: LocalDateTime) = apply { this.startAt = startAt }
        fun endAt(endAt: LocalDateTime) = apply { this.endAt = endAt }

        fun build(): Auction {
            val auction = Auction()
            auction.seller = requireNotNull(seller)
            auction.category = requireNotNull(category)
            auction.name = requireNotNull(name)
            auction.description = description
            auction.startPrice = requireNotNull(startPrice)
            auction.buyNowPrice = buyNowPrice
            auction.startAt = requireNotNull(startAt)
            auction.endAt = requireNotNull(endAt)
            auction.bidCount = 0
            auction.status = AuctionStatus.OPEN
            return auction
        }
    }
}
