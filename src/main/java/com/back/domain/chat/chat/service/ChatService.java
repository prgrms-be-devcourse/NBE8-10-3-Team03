package com.back.domain.chat.chat.service;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.auction.auction.service.FileStorageService;
import com.back.domain.chat.chat.dto.request.ChatMessageRequest;
import com.back.domain.chat.chat.dto.response.ChatIdResponse;
import com.back.domain.chat.chat.dto.response.ChatResponse;
import com.back.domain.chat.chat.dto.response.ChatRoomIdResponse;
import com.back.domain.chat.chat.entity.Chat;
import com.back.domain.chat.chat.entity.ChatImage;
import com.back.domain.chat.chat.entity.ChatRoom;
import com.back.domain.chat.chat.entity.ChatRoomType;
import com.back.domain.chat.chat.repository.ChatImageRepository;
import com.back.domain.chat.chat.repository.ChatRepository;
import com.back.domain.chat.chat.repository.ChatRoomRepository;
import com.back.domain.image.image.entity.Image;
import com.back.domain.image.image.repository.ImageRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.entity.PostStatus;
import com.back.domain.post.post.repository.PostRepository;
import com.back.global.rsData.RsData;
import com.back.global.exception.ServiceException; // ServiceException이 있다고 가정
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final ImageRepository imageRepository;
    private final ChatImageRepository chatImageRepository;
    private final FileStorageService fileStorageService;

    /**
     * * 채팅방 생성
     * @param itemId 상품 ID (Post ID or Auction ID)
     * @param txType POST or AUCTION
     * @param buyerApiKey 구매자 API Key
     * @return roomId (UUID)
     **/
    @Transactional
    public RsData<ChatRoomIdResponse> createChatRoom(int itemId, String txType, String buyerApiKey) {
        ChatRoomType type = ChatRoomType.valueOf(txType.toUpperCase());

        Member buyer = memberRepository.findByApiKey(buyerApiKey)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));

        Member seller;
        Post post = null;
        Auction auction = null;

        if (type == ChatRoomType.POST) {
            post = postRepository.findById(itemId)
                    .orElseThrow(() -> new ServiceException("404-2", "해당 게시글이 존재하지 않습니다."));

            if (post.getStatus() != PostStatus.SALE) {
                throw new ServiceException("400-1", "판매 중인 상품이 아니므로 채팅을 시작할 수 없습니다.");
            }
            seller = post.getSeller();

        } else if (type == ChatRoomType.AUCTION) {
            auction = auctionRepository.findById(itemId)
                    .orElseThrow(() -> new ServiceException("404-3", "존재하지 않는 경매입니다."));

            seller = auction.getSeller();

        } else {
            throw new ServiceException("400-2", "채팅을 개설할 수 없습니다.");
        }

        if (seller.getApiKey().equals(buyerApiKey)) {
            throw new ServiceException("400-3", "본인의 상품에는 채팅을 개설할 수 없습니다.");
        }

        String roomId;
        if (type == ChatRoomType.POST) {
            final Post finalPost = post;
            roomId = chatRoomRepository.findByPostAndBuyerId(post, buyerApiKey)
                    .map(ChatRoom::getRoomId)
                    .orElseGet(() -> {
                        ChatRoom room = ChatRoom.createForPost(finalPost, buyer);
                        return chatRoomRepository.save(room).getRoomId();
                    });
        } else {
            final Auction finalAuction = auction;
            roomId = chatRoomRepository.findByAuctionAndBuyerId(auction, buyerApiKey)
                    .map(ChatRoom::getRoomId)
                    .orElseGet(() -> {
                        ChatRoom room = ChatRoom.createForAuction(finalAuction, buyer);
                        return chatRoomRepository.save(room).getRoomId();
                    });
        }

        return new RsData<>("200-1", "채팅방에 입장했습니다.", new ChatRoomIdResponse(roomId));
    }

    @Transactional
    public RsData<ChatIdResponse> saveMessage(ChatMessageRequest req) {
        ChatRoom room = chatRoomRepository.findByRoomId(req.getRoomId())
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 채팅방입니다."));

        Chat chatMessage = Chat.builder()
                .chatRoom(room)
                .sender(req.getSender())
                .message(req.getMessage())
                .isRead(false)
                .build();

        chatRepository.save(chatMessage);

        if (req.getImages() != null) {
            for (MultipartFile file : req.getImages()) {
                if (file.isEmpty()) continue;

                String imageUrl = fileStorageService.storeFile(file);
                Image savedImage = imageRepository.save(new Image(imageUrl));

                ChatImage chatImage = new ChatImage(chatMessage, savedImage);
                chatMessage.addChatImage(chatImage);
            }
        }

        return new RsData<>("200-1", "메시지가 전송되었습니다.", new ChatIdResponse(chatMessage.getId()));
    }

    @Transactional
    public RsData<List<ChatResponse>> getMessages(String roomId, Integer lastChatId, String readerName) {
        if (readerName != null) {
            chatRepository.markMessagesAsRead(roomId, readerName);
        }

        List<Chat> chats;
        if (lastChatId == null || lastChatId <= 0) {
            chats = chatRepository.findAllByChatRoom_RoomIdOrderByCreateDateAsc(roomId);
        } else {
            chats = chatRepository.findByChatRoom_RoomIdAndIdGreaterThanOrderByCreateDateAsc(roomId, lastChatId);
        }

        List<ChatResponse> responses = chats.stream()
                .map(ChatResponse::new)
                .collect(Collectors.toList());

        return new RsData<>("200-1", "메시지 조회 성공", responses);
    }

    public RsData<List<ChatResponse>> getChatList() {
        List<ChatResponse> responses = chatRepository.findAllLatestMessages().stream()
                .map(ChatResponse::new)
                .collect(Collectors.toList());

        return new RsData<>("200-1", "채팅 목록 조회 성공", responses);
    }
}