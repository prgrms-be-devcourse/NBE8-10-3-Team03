package com.back.domain.chat.chat.service;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.auction.auction.service.FileStorageService;
import com.back.domain.chat.chat.dto.request.ChatMessageRequest;
import com.back.domain.chat.chat.dto.response.ChatIdResponse;
import com.back.domain.chat.chat.dto.response.ChatResponse;
import com.back.domain.chat.chat.dto.response.ChatRoomIdResponse;
import com.back.domain.chat.chat.dto.response.ChatRoomListResponse;
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
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
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
    private final SimpMessagingTemplate messagingTemplate;
    private final Rq rq;

    /**
     * * 채팅방 생성
     * @param itemId 상품 ID (Post ID or Auction ID)
     * @param txType POST or AUCTION
     * @return roomId (UUID)
     **/
    @Transactional
    public RsData<ChatRoomIdResponse> createChatRoom(int itemId, String txType) {
        ChatRoomType type = ChatRoomType.valueOf(txType.toUpperCase());

        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException("401-1", "로그인이 필요합니다.");
        }

        Member buyer = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));

        String buyerApiKey = buyer.getApiKey();

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

        // Rq Actor ID로 완전한 Member 조회 후 API Key 확보
        Member actor = rq.getActor();
        Member sender = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));

        String currentApiKey = sender.getApiKey();

        // 참여자 권한체크
        if (!room.getSellerId().equals(currentApiKey) && !room.getBuyerId().equals(currentApiKey)) {
            throw new ServiceException("403-1", "해당 채팅방에 메세지를 보낼 권한이 없습니다.");
        }

        // 메세지 저장
        Chat chatMessage = Chat.builder()
                .chatRoom(room)
                .senderId(sender.getId())
                .message(req.getMessage())
                .isRead(false)
                .build();

        chatRepository.save(chatMessage);

        // 이미지 파일 처리
        if (req.getImages() != null) {
            for (MultipartFile file : req.getImages()) {
                if (file.isEmpty()) continue;

                String imageUrl = fileStorageService.storeFile(file);
                Image savedImage = imageRepository.save(new Image(imageUrl));

                ChatImage chatImage = new ChatImage(chatMessage, savedImage);
                chatMessage.addChatImage(chatImage);
            }
        }

        chatRepository.save(chatMessage);

        ChatResponse chatResponse = new ChatResponse(chatMessage);

        // "/sub/chat/room/{roomId}" 를 구독(Sub) 중인 모든 클라이언트에게 전송
        messagingTemplate.convertAndSend("/sub/chat/room/" + req.getRoomId(), chatResponse);

        return new RsData<>("200-1", "메시지가 전송되었습니다.", new ChatIdResponse(chatMessage.getId()));
    }

    @Transactional
    public RsData<List<ChatResponse>> getMessages(String roomId, Integer lastChatId) {
        ChatRoom room = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 채팅방입니다."));

        Member actor = rq.getActor();
        Member reader = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));

        String currentApiKey = reader.getApiKey();

        // 참여자만 메시지 조회 가능
        if (!room.getSellerId().equals(currentApiKey) && !room.getBuyerId().equals(currentApiKey)) {
            throw new ServiceException("403-1", "해당 채팅방에 접근 권한이 없습니다.");
        }

        chatRepository.markMessagesAsRead(roomId, actor.getId());

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

    public RsData<List<ChatRoomListResponse>> getChatList() {
        Member actor = rq.getActor();
        Member me = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));

        String myApiKey = me.getApiKey();

        List<Chat> latestChats = chatRepository.findAllLatestMessagesByMember(myApiKey);

        List<ChatRoomListResponse> responseList = new ArrayList<>();

        for (Chat chat : latestChats) {
            ChatRoom room = chat.getChatRoom();

            // 상대방 찾기 (myApiKey와 apiKey가 다른 사람이 상대방)
            String opponentApiKey = room.getSellerId().equals(myApiKey) ? room.getBuyerId() : room.getSellerId();
            Member opponent = memberRepository.findByApiKey(opponentApiKey)
                    .orElseThrow(() -> new ServiceException("404-1", "상대방 정보를 찾을 수 없습니다."));

            // 안 읽은 메세지 수
            Integer unreadCount = chatRepository.countByChatRoom_RoomIdAndIsReadFalseAndSenderIdNot(room.getRoomId(), me.getId());

            // 상품 정보
            String itemName = "";
            String itemImageUrl = "";
            int itemPrice = 0;
            Integer itemId = 0;

            // 거래 종류가 일반 거래 일 경우
            if (room.getTxType() == ChatRoomType.POST && room.getPost() != null) {
                Post post = room.getPost();
                itemId = post.getId();
                itemName = post.getTitle();
                itemPrice = post.getPrice();
                // TODO: Post에 대표 이미지 필드가 있다면 가져오기 (예: post.getThumbnailUrl())
            }
            // 거래 종류가 경매 일 경우
            else if (room.getTxType() == ChatRoomType.AUCTION && room.getAuction() != null) {
                Auction auction = room.getAuction();
                itemId = auction.getId();
                itemName = auction.getName();
                itemPrice = auction.getCurrentHighestBid();
                // TODO: Post에 대표 이미지 필드가 있다면 가져오기 (예: auction.getThumbnailUrl())
            }

            // DTO 조립
            responseList.add(ChatRoomListResponse.builder()
                    .roomId(room.getRoomId())
                    .opponentId(opponent.getId())
                    .opponentNickname(opponent.getNickname())
                    .opponentProfileImageUrl(opponent.getProfileImgUrl())
                    .lastMessage(chat.getMessage())
                    .lastMessageDate(chat.getCreateDate())
                    .unreadCount(unreadCount)
                    .itemId(itemId)
                    .itemName(itemName)
                    .itemImageUrl(itemImageUrl)
                    .itemPrice(itemPrice)
                    .txType(room.getTxType())
                    .build());
        }
        return new RsData<>("200-1", "채팅 목록 조회 성공", responseList);
    }

    @Transactional
    public RsData<Void> exitChatRoom(String roomId) {
        ChatRoom room = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 채팅방입니다."));

        Member actor = rq.getActor();
        Member me = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));

        String currentApiKey = me.getApiKey();

        if (!room.getSellerId().equals(currentApiKey) && !room.getBuyerId().equals(currentApiKey)) {
            throw new ServiceException("403-1", "해당 채팅방에 접근 권한이 없습니다.");
        }

        room.exit(currentApiKey);

        if (room.isBothExited()) {
            chatRoomRepository.delete(room);
        }

        return new RsData<>("200-1", "채팅방에서 퇴장하였습니다.", null);
    }
}