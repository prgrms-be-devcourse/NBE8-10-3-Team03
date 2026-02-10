package com.back.domain.chat.chat.repository;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.chat.chat.entity.ChatRoom;
import com.back.domain.post.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Integer> {

    // UUID로 방 찾기 (삭제되지 않은 방만)
    Optional<ChatRoom> findByRoomIdAndDeletedFalse(String roomId);

    // POST: 이미 존재하는 방인지 확인 (삭제되지 않은 방만)
    Optional<ChatRoom> findByPostAndBuyerApiKeyAndDeletedFalse(Post post, String buyerId);

    // Auction: 이미 존재하는 방인지 확인 (삭제되지 않은 방만)
    Optional<ChatRoom> findByAuctionAndBuyerApiKeyAndDeletedFalse(Auction auction, String buyerId);
}