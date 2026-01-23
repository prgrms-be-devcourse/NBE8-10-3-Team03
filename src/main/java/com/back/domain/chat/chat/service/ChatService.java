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
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
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
                .orElseThrow(() -> {
                    log.warn("메세지 전송 실패 - 존재하지 않는 채팅방: {}",  req.getRoomId());
                    return new ServiceException("404-1", "존재하지 않는 채팅방입니다.");
                });

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

        // "/sub/v1/chat/room/{roomId}" 를 구독(Sub) 중인 모든 클라이언트에게 전송
        try {
            messagingTemplate.convertAndSend("/sub/v1/chat/room/" + req.getRoomId(), chatResponse);
            log.info("메시지 브로드캐스팅 성공 - RoomID: {}, MessageID: {}", req.getRoomId(), chatMessage.getId());
        } catch (Exception e) {
            log.error("WebSocket 전송 실패 - RoomID: {}, Error: {}", req.getRoomId(), e.getMessage());
        }

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
        Integer myId = me.getId();

        List<Chat> latestChats = chatRepository.findAllLatestMessagesByMember(myApiKey);

        if (latestChats.isEmpty()) {
            return new RsData<>("200-1", "채팅 목록 조회 성공", new ArrayList<>());
        }

        // 채팅방 ID 및 상대방 API Key 수집
        // roomIds: 안 읽은 메시지 개수 일괄 조회용
        // opponentApiKeys: 상대방 프로필 정보 일괄 조회용
        Set<String> roomIds = new HashSet<>();
        Set<String> opponentApiKeys = new HashSet<>();

        for (Chat chat : latestChats) {
            ChatRoom room = chat.getChatRoom();
            roomIds.add(room.getRoomId());

            // 상대방 찾기 (내가 구매자면 상대방은 판매자 => 반대)
            String opponentKey = room.getSellerId().equals(myApiKey) ? room.getBuyerId() : room.getSellerId();
            opponentApiKeys.add(opponentKey);
        }

        // 상대방 정보 일괄 조회 & Map 변환
        Map<String, Member> opponentMap = memberRepository.findByApiKeyIn(opponentApiKeys).stream()
                .collect(Collectors.toMap(Member::getApiKey, Function.identity()));

        // 안 읽은 메시지 개수 일괄 조회하고 Map으로 변환
        Map<String, Integer> unreadCountMap = chatRepository.countUnreadMessagesByRoomIds(roomIds, myId).stream()
                .collect(Collectors.toMap(
                        result -> (String) result[0], // 키 : roomId
                        result -> (Integer) result[1] // 값 : count
                ));

        List<ChatRoomListResponse> responseList = new ArrayList<>();

        // 응답 DTO 조립
        for (Chat chat : latestChats) {
            ChatRoom room = chat.getChatRoom();
            String currentRoomId = room.getRoomId();

            // 상대방 정보 가져오기
            String opponentKey = room.getSellerId().equals(myApiKey) ? room.getBuyerId() : room.getSellerId();
            Member opponent = opponentMap.get(opponentKey);

            if (opponent == null) continue;

            // 안 읽은 메시지 수 가져오기 없으면 0
            int unreadCount = unreadCountMap.getOrDefault(currentRoomId, 0);

            // 상품 정보 추출
            String itemName = "";
            String itemImageUrl = "";
            int itemPrice = 0;
            Integer itemId = 0;

            if (room.getTxType() == ChatRoomType.POST && room.getPost() != null) {
                Post post = room.getPost();
                itemId = post.getId();
                itemName = post.getTitle();
                itemPrice = post.getPrice();
                // TODO: Post에 대표 이미지 필드가 있다면 가져오기
            }
            else if (room.getTxType() == ChatRoomType.AUCTION && room.getAuction() != null) {
                Auction auction = room.getAuction();
                itemId = auction.getId();
                itemName = auction.getName();
                itemPrice = auction.getCurrentHighestBid();
                // TODO: Auction에 대표 이미지 필드가 있다면 가져오기
            }

            // DTO 조립
            responseList.add(ChatRoomListResponse.builder()
                    .roomId(currentRoomId)
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

        log.debug("채팅 목록 조회 완료 - 사용자: {}, 조회된 방 개수: {}", me.getNickname(), responseList.size());
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