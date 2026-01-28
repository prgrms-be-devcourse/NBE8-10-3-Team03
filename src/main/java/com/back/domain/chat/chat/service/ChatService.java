package com.back.domain.chat.chat.service;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.auction.auction.service.FileStorageService;
import com.back.domain.chat.chat.dto.request.ChatMessageRequest;
import com.back.domain.chat.chat.dto.response.*;
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
import java.util.concurrent.CompletableFuture;
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
     * 채팅방 생성 (또는 기존 방 반환)
     * 1. 로그인 사용자 검증 및 구매자 정보 확보
     * 2. 거래 타입(일반/경매)에 따른 상품 존재 여부 및 판매 가능 상태 확인
     * 3. 본인 상품 여부 체크 (자가 거래 방지)
     * 4. 기존 동일 상품/참여자 기반 채팅방 존재 시 해당 방 ID 반환, 없을 시 생성
     */
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

        // 거래 대상(POST or AUCTION)에 따른 데이터 검증
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

        // 판매자가 본인 상품에 채팅방을 만드는 것을 금지
        if (seller.getApiKey().equals(buyerApiKey)) {
            throw new ServiceException("400-3", "본인의 상품에는 채팅을 개설할 수 없습니다.");
        }

        // 기존에 대화하던 방이 있으면 UUID를 그대로 반환 없으면 신규 생성
        String roomId;
        if (type == ChatRoomType.POST) {
            final Post finalPost = post;
            roomId = chatRoomRepository.findByPostAndBuyerApiKey(post, buyerApiKey)
                    .map(ChatRoom::getRoomId)
                    .orElseGet(() -> {
                        ChatRoom room = ChatRoom.createForPost(finalPost, buyer);
                        return chatRoomRepository.save(room).getRoomId();
                    });
        } else {
            final Auction finalAuction = auction;
            roomId = chatRoomRepository.findByAuctionAndBuyerApiKey(auction, buyerApiKey)
                    .map(ChatRoom::getRoomId)
                    .orElseGet(() -> {
                        ChatRoom room = ChatRoom.createForAuction(finalAuction, buyer);
                        return chatRoomRepository.save(room).getRoomId();
                    });
        }

        return new RsData<>("200-1", "채팅방에 입장했습니다.", new ChatRoomIdResponse(roomId));
    }

    /**
     * 메시지 전송 및 실시간 브로드캐스팅
     * 1. 채팅방 및 발신자 권한 확인
     * 2. 메시지 엔티티 저장 (Chat)
     * 3. 이미지 첨부 시 파일 저장 및 매핑 정보 생성 (ChatImage)
     * 4. STOMP(WebSocket)를 사용하여 해당 방을 구독 중인 클라이언트들에게 실시간 전송
     */
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
        if (!room.getSellerApiKey().equals(currentApiKey) && !room.getBuyerApiKey().equals(currentApiKey)) {
            throw new ServiceException("403-1", "해당 채팅방에 메세지를 보낼 권한이 없습니다.");
        }

        // 메세지 정보 구축
        Chat chatMessage = Chat.builder()
                .chatRoom(room)
                .senderId(sender.getId())
                .message(req.getMessage())
                .read(false)
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
        // 실시간 대화창 표시를 위해 발신자의 프로필 이미지 정보 추가
        chatResponse.setSenderProfileImageUrl(sender.getProfileImgUrl());

        // "/sub/v1/chat/room/{roomId}" 를 구독(Sub) 중인 모든 클라이언트에게 전송
        try {
            messagingTemplate.convertAndSend("/sub/v1/chat/room/" + req.getRoomId(), chatResponse);
            log.info("메시지 브로드캐스팅 성공 - RoomID: {}, MessageID: {}", req.getRoomId(), chatMessage.getId());
        } catch (Exception e) {
            log.error("WebSocket 전송 실패 - RoomID: {}, Error: {}", req.getRoomId(), e.getMessage());
        }

        return new RsData<>("200-1", "메시지가 전송되었습니다.", new ChatIdResponse(chatMessage.getId()));
    }

    /**
     * 채팅 내역 조회
     * 1. 채팅방 정보 및 참여자(판매자/구매자) 최신 정보 확보
     * 2. 상대방이 보낸 메시지들에 대한 일괄 읽음 처리
     * 3. 페이징 처리: lastChatId 기준으로 이전 혹은 이후 내역 조회
     * 4. 각 메시지 DTO에 발신자 프로필 이미지 매핑 (UI용)
     */
    @Transactional
    public RsData<List<ChatResponse>> getMessages(String roomId, Integer lastChatId) {
        ChatRoom room = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 채팅방입니다."));

        // 접속 사용자 권한 검증
        Member actor = rq.getActor();
        Member me = memberRepository.findById(actor.getId()).orElseThrow();
        String currentApiKey = me.getApiKey();

        if (!room.getSellerApiKey().equals(currentApiKey) && !room.getBuyerApiKey().equals(currentApiKey)) {
            throw new ServiceException("403-1", "해당 채팅방에 접근 권한이 없습니다.");
        }

        // 확인한 상대방의 메시지들을 읽음 처리 및 WebSocket 알림
        int updatedCount = chatRepository.markMessagesAsRead(roomId, me.getId());
        if (updatedCount > 0) {
            // 상대방에게 읽음 알림 전송
            Map<String, Object> readNotification= new HashMap<>();
            readNotification.put("readerId", me.getId());
            readNotification.put("roomId", roomId);
            messagingTemplate.convertAndSend("/sub/v1/chat/room/" + roomId + "/read", (Object) readNotification);
            log.debug("읽음 알림 전송 - RoomId: {}, ReaderId: {}, UpdatedCount: {}", roomId, me.getId(), updatedCount);
        }

        // 최신 프로필 이미지를 보여주기 위해 Member 정보를 미리 조회
        Member seller = memberRepository.findByApiKey(room.getSellerApiKey()).orElse(null);
        Member buyer = memberRepository.findByApiKey(room.getBuyerApiKey()).orElse(null);

        // No-Offset 페이징 로직
        List<Chat> chats;
        // 방 처음 진입 시 가장 최신 메세지 20개 조회 (내림차순)
        if (lastChatId == null || lastChatId <= 0) {
            chats = chatRepository.findTop20ByChatRoom_RoomIdOrderByIdDesc(roomId);
        } else {
            // 스크롤 상단 도달 시 특정 ID 이전의 과거 데이터 20개 조회 (내림차순)
            chats = chatRepository.findTop20ByChatRoom_RoomIdAndIdLessThanOrderByIdDesc(roomId, lastChatId);
        }

        Collections.reverse(chats);

        // SenderId를 비교하여 Sender를 찾고 senderProfileImageUrl에 이미지 URL 주입
        List<ChatResponse> responses = chats.stream()
                .map(chat -> {
                    ChatResponse res = new ChatResponse(chat);
                    if (seller != null && chat.getSenderId().equals(seller.getId())) {
                        res.setSenderProfileImageUrl(seller.getProfileImgUrl());
                    } else if (buyer != null && chat.getSenderId().equals(buyer.getId())) {
                        res.setSenderProfileImageUrl(buyer.getProfileImgUrl());
                    }
                    return res;
                })
                .collect(Collectors.toList());

        return new RsData<>("200-1", "메시지 조회 성공", responses);
    }

    /**
     * 나의 전체 채팅 목록 조회
     * 1. 내가 속한 모든 채팅방의 '마지막 메시지'들 조회
     * 2. 상대방 정보(닉네임, 프로필) 일괄 조회 및 매핑
     * 3. 각 방별 '안 읽은 메시지 수' 일괄 카운트
     * 4. 관련 상품(Post/Auction) 정보 및 썸네일 이미지 추출
     */
    public RsData<List<ChatRoomListResponse>> getChatList() {
        Member actor = rq.getActor();
        Member me = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));

        String myApiKey = me.getApiKey();

        // 참여한 방들의 최신 메시지 조회
        List<Chat> latestChats = chatRepository.findAllLatestChatsByMember(myApiKey);

        if (latestChats.isEmpty()) {
            return new RsData<>("200-1", "채팅 목록 조회 성공", new ArrayList<>());
        }

        // ID 수집 (비동기 조회를 위한 준비)
        List<String> roomIds = new ArrayList<>();
        Set<String> opponentApiKeys = new HashSet<>();
        List<Integer> postIds = new ArrayList<>();
        List<Integer> auctionIds = new ArrayList<>();

        for (Chat chat : latestChats) {
            ChatRoom room = chat.getChatRoom();
            roomIds.add(room.getRoomId());
            opponentApiKeys.add(room.getSellerApiKey().equals(myApiKey) ? room.getBuyerApiKey() : room.getSellerApiKey());

            if (room.getTxType() == ChatRoomType.POST && room.getPost() != null) {
                postIds.add(room.getPost().getId());
            } else if (room.getTxType() == ChatRoomType.AUCTION && room.getAuction() != null) {
                auctionIds.add(room.getAuction().getId());
            }
        }

        final Integer myId = me.getId();
        final List<String> roomIdsList = new ArrayList<>(roomIds);
        final Set<String> opponentKeysSet = opponentApiKeys;

        // [Async] 병렬 데이터 로딩 (안 읽은 개수, 회원 정보, 이미지들)
        // CompletableFuture를 사용하여 쿼리를 동시에 실행
        // 안 읽은 메시지 개수 조회
        CompletableFuture<Map<String, Integer>> unreadFuture = CompletableFuture.supplyAsync(() ->
                chatRepository.countUnreadMessagesByRoomIds(roomIdsList, myId).stream()
                        .collect(Collectors.toMap(
                                UnreadCountResponse::roomId,
                                dto -> dto.count().intValue(),
                                (oldVal, newVal) -> oldVal // [중요] 중복 키 발생 시 기존 값 유지
                        ))
        );

        // 상대방 Member 정보 조회
        CompletableFuture<Map<String, Member>> memberFuture = CompletableFuture.supplyAsync(() ->
                memberRepository.findByApiKeyIn(opponentKeysSet).stream()
                        .collect(Collectors.toMap(
                                Member::getApiKey,
                                Function.identity(),
                                (oldVal, newVal) -> oldVal // 중복 키 방지
                        ))
        );

        // 엔티티를 수정하지 않기 위해 Repository의 벌크 쿼리(Object[])를 사용
        CompletableFuture<Map<Integer, String>> postImageFuture = CompletableFuture.supplyAsync(() ->
                imageRepository.findPostMainImages(postIds).stream()
                        .collect(Collectors.toMap(obj -> (Integer) obj[0], obj -> (String) obj[1], (a, b) -> a))
        );

        CompletableFuture<Map<Integer, String>> auctionImageFuture = CompletableFuture.supplyAsync(() ->
                imageRepository.findAuctionMainImages(auctionIds).stream()
                        .collect(Collectors.toMap(obj -> (Integer) obj[0], obj -> (String) obj[1], (a, b) -> a))
        );

        Map<String, Integer> unreadCountMap = unreadFuture.join();
        Map<String, Member> opponentMap = memberFuture.join();
        Map<Integer, String> postImageMap = postImageFuture.join();
        Map<Integer, String> auctionImageMap = auctionImageFuture.join();

        // 메모리 조합 및 DTO 변환
        List<ChatRoomListResponse> responseList = latestChats.stream()
                .map(chat -> {
                    ChatRoom room = chat.getChatRoom();
                    String opponentKey = room.getSellerApiKey().equals(myApiKey) ? room.getBuyerApiKey() : room.getSellerApiKey();
                    Member opponent = opponentMap.get(opponentKey);

                    if (opponent == null) return null;

                    Integer itemId = null;
                    String itemName = "";
                    String itemImageUrl = null;
                    Integer itemPrice = 0;

                    // POST 처리
                    if (room.getTxType() == ChatRoomType.POST && room.getPost() != null) {
                        Post post = room.getPost();
                        itemId = post.getId();
                        itemName = post.getTitle();
                        itemPrice = post.getPrice();
                        itemImageUrl = postImageMap.get(itemId);
                    }
                    // AUCTION 처리
                    else if (room.getTxType() == ChatRoomType.AUCTION && room.getAuction() != null) {
                        Auction auction = room.getAuction();
                        itemId = auction.getId();
                        itemName = auction.getName();
                        // 입찰가 우선, 없으면 시작가
                        itemPrice = auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : auction.getStartPrice();
                        itemImageUrl = auctionImageMap.get(itemId);
                    }

                    Double opponentReputation = (opponent.getReputation() != null) ? opponent.getReputation().getScore() : 50.0;

                    return ChatRoomListResponse.builder()
                            .roomId(room.getRoomId())
                            .opponentId(opponent.getId())
                            .opponentNickname(opponent.getNickname())
                            .opponentProfileImageUrl(opponent.getProfileImgUrl())
                            .opponentReputation(opponentReputation)
                            .lastMessage(chat.getMessage())
                            .lastMessageDate(chat.getCreateDate())
                            .unreadCount(unreadCountMap.getOrDefault(room.getRoomId(), 0))
                            .itemId(itemId)
                            .itemName(itemName)
                            .itemImageUrl(itemImageUrl)
                            .itemPrice(itemPrice)
                            .txType(room.getTxType())
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ChatRoomListResponse::getLastMessageDate).reversed()) // 최신순 재정렬
                .collect(Collectors.toList());

        log.debug("채팅 목록 조회 완료 - 사용자: {}, 조회된 방 개수: {}", me.getNickname(), responseList.size());
        return new RsData<>("200-1", "채팅 목록 조회 성공", responseList);
    }


    /**
     * 채팅방 퇴장
     * 1. 참여자(판매자 혹은 구매자) 중 한 명이 퇴장했음을 표시
     * 2. 데이터 보존: 한 명이 나갔다고 해서 즉시 DB에서 방을 지우지 않음 (상대방은 대화 내용을 봐야 하므로)
     * 3. 삭제 처리: 두 사람 모두가 방에서 나갔을 경우에만 DB에서 실제 레코드 삭제
     */
    @Transactional
    public RsData<Void> exitChatRoom(String roomId) {
        ChatRoom room = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 채팅방입니다."));

        Member actor = rq.getActor();
        Member me = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));

        String currentApiKey = me.getApiKey();

        if (!room.getSellerApiKey().equals(currentApiKey) && !room.getBuyerApiKey().equals(currentApiKey)) {
            throw new ServiceException("403-1", "해당 채팅방에 접근 권한이 없습니다.");
        }

        room.exit(currentApiKey);

        if (room.isBothExited()) {
            chatRoomRepository.delete(room);
        }

        return new RsData<>("200-1", "채팅방에서 퇴장하였습니다.", null);
    }
}