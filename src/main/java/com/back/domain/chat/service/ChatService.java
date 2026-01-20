package com.back.domain.chat.service;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.chat.dto.ChatDto;
import com.back.domain.chat.entity.Chat;
import com.back.domain.chat.entity.ChatRoom;
import com.back.domain.chat.entity.ChatRoomType;
import com.back.domain.chat.repository.ChatRepository;
import com.back.domain.chat.repository.ChatRoomRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.entity.PostStatus;
import com.back.domain.post.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final PostRepository postRepository;
    private final AuctionRepository auctionRepository;
    private final MemberRepository memberRepository;

    /**
     * 채팅방 생성
     * @param itemId 상품 ID (Post ID or Auction ID)
     * @param txType POST or AUCTION
     * @param buyerApiKey 구매자 API Key
     * @return roomId (UUID)
     */
    @Transactional
    public String createChatRoom(int itemId, String txType, String buyerApiKey) {
        ChatRoomType type = ChatRoomType.valueOf(txType.toUpperCase());

        Member buyer = memberRepository.findByApiKey(buyerApiKey)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (type == ChatRoomType.POST) {
            Post post = postRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

            // SALE 상태가 아니면 채팅방 생성 불가능
            if (post.getStatus() != PostStatus.SALE) {
                throw new IllegalStateException("판매 중인 상품이 아니므로 채팅을 시작할 수 없습니다.");
            }

            // 셀프 채팅 방지
            if (post.getSeller().getApiKey().equals(buyerApiKey)) {
                throw new IllegalArgumentException("본인의 상품에는 채팅을 개설할 수 없습니다.");
            }

            // 이미 존재하는 방인지 확인 후 없으면 생성
            return chatRoomRepository.findByPostAndBuyerId(post, buyerApiKey)
                    .map(ChatRoom::getRoomId)
                    .orElseGet(() -> {
                        ChatRoom room = ChatRoom.createForPost(post, buyer);
                        chatRoomRepository.save(room);
                        return room.getRoomId();
                    });

        } else if (type == ChatRoomType.AUCTION) {
            Auction auction = auctionRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 경매입니다."));

            return chatRoomRepository.findByAuctionAndBuyerId(auction, buyerApiKey)
                    .map(ChatRoom::getRoomId)
                    .orElseGet(() -> {
                        ChatRoom room = ChatRoom.createForAuction(auction, buyer);
                        chatRoomRepository.save(room);
                        return room.getRoomId();
                    });

        } else {
            throw new IllegalArgumentException("채팅을 개설할 수 없습니다.");
        }
    }

    @Transactional
    public void saveMessage(ChatDto chatDto) {
        // 방 존재 여부 검증
        ChatRoom room = chatRoomRepository.findByRoomId(chatDto.roomId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        Chat chatMessage = Chat.builder()
                .chatRoom(room)
                .sender(chatDto.sender())
                .message(chatDto.message())
                .isRead(false)
                .build();

        chatRepository.save(chatMessage);
    }

    @Transactional
    public List<ChatDto> getMessages(String roomId, Integer lastChatId, String readerName) {
        if (readerName != null) {
            chatRepository.markMessagesAsRead(roomId, readerName);
        }

        List<Chat> chats;
        if (lastChatId == null || lastChatId <= 0) {
            chats = chatRepository.findAllByRoomIdOrderByCreateDateAsc(roomId);
        } else {
            chats = chatRepository.findByRoomIdAndIdGreaterThanOrderByCreateDateAsc(roomId, lastChatId);
        }

        return chats.stream()
                .map(this::chatDto)
                .collect(Collectors.toList());
    }

    public List<ChatDto> getChatList() {
        return chatRepository.findAllLatestMessages().stream()
                .map(this::chatDto)
                .collect(Collectors.toList());
    }

    private ChatDto chatDto(Chat chat) {
        ChatRoom room = chat.getChatRoom();

        Integer itemId = null;
        if (room.getTxType() == ChatRoomType.POST) {
            itemId = (room.getPost() != null) ? room.getPost().getId() : null;
        } else if (room.getTxType() == ChatRoomType.AUCTION) {
            itemId = (room.getAuction() != null) ? room.getAuction().getId() : null;
        }

        return new ChatDto(
                chat.getId(),
                itemId,
                room.getRoomId(),
                chat.getSender(),
                chat.getMessage(),
                chat.getCreateDate(),
                chat.isRead());
    }
}