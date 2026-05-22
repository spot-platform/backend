package backend.chat.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import backend.chat.dto.ChatBlockResponse;
import backend.chat.dto.ChatMessageListResponse;
import backend.chat.dto.ChatMessageResponse;
import backend.chat.dto.ChatRoomResponse;
import backend.chat.dto.ChatRoomResponse.ChatRoomEnrichment;
import backend.chat.dto.CreateChatRoomRequest;
import backend.chat.dto.CreatePersonalChatRoomRequest;
import backend.chat.dto.SendMessageRequest;
import backend.chat.entity.ChatBlock;
import backend.chat.entity.ChatMessage;
import backend.chat.entity.ChatMessageType;
import backend.chat.entity.ChatRoom;
import backend.chat.entity.ChatRoomMember;
import backend.chat.entity.ChatRoomType;
import backend.chat.repository.ChatBlockRepository;
import backend.chat.repository.ChatMessageRepository;
import backend.chat.repository.ChatRoomMemberRepository;
import backend.chat.repository.ChatRoomRepository;
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import backend.spot.entity.Spot;
import backend.spot.repository.SpotRepository;
import backend.user.entity.UserEntity;
import backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatBlockRepository chatBlockRepository;
	private final SseEmitterService sseEmitterService;
	private final SpotRepository spotRepository;
	private final UserRepository userRepository;

	// ─────────────────────────────────────────────
	// 채팅방 (Room)
	// ─────────────────────────────────────────────

	/**
	 * 전체 채팅방 목록을 조회합니다.
	 * 인증된 유저만 호출 가능 — 본인이 멤버로 속한 방만 반환합니다.
	 */
	@Transactional(readOnly = true)
	public List<ChatRoomResponse> getRooms() {
		return getRooms(null);
	}

	@Transactional(readOnly = true)
	public List<ChatRoomResponse> getRooms(String currentUserId) {
		UserEntity currentUser = findCurrentUser(currentUserId);
		List<ChatRoom> rooms;
		if (currentUser == null) {
			// 비인증 — 멤버십 기반 필터가 불가능하므로 빈 리스트 반환
			rooms = List.of();
		} else {
			List<Long> roomIds = chatRoomMemberRepository.findChatRoomIdsByUserId(currentUser.getId());
			rooms = roomIds.isEmpty()
				? List.of()
				: chatRoomRepository.findAllById(roomIds).stream()
					.filter(r -> !r.isDeleted())
					.toList();
		}
		Map<Long, ChatRoomEnrichment> enrichments = buildEnrichments(rooms, currentUser);
		return rooms.stream()
			.map(room -> ChatRoomResponse.from(room, enrichments.getOrDefault(room.getId(), ChatRoomEnrichment.empty())))
			.toList();
	}

	/**
	 * 그룹 채팅방을 생성합니다. (PERSONAL 은 createPersonalRoom 사용)
	 * 동일 spotId 의 GROUP 방이 이미 있으면 idempotent 하게 기존 방을 반환합니다.
	 */
	public ChatRoomResponse createRoom(CreateChatRoomRequest request) {
		return createRoom(request, null);
	}

	public ChatRoomResponse createRoom(CreateChatRoomRequest request, String currentUserId) {
		if (request.getType() == ChatRoomType.PERSONAL) {
			throw new BusinessException(ErrorCode.CHAT_PERSONAL_REQUIRES_PARTNER);
		}
		if (request.getType() == ChatRoomType.GROUP && request.getSpotId() == null) {
			throw new BusinessException(ErrorCode.GROUP_CHAT_REQUIRES_SPOT);
		}

		// GROUP 생성 시 spotId 실존 검증 — 없는 Spot이면 404
		if (request.getType() == ChatRoomType.GROUP
				&& !spotRepository.existsById(request.getSpotId())) {
			throw new BusinessException(ErrorCode.SPOT_NOT_FOUND);
		}

		// 동일 spot 의 활성 GROUP 방 idempotent 재사용 (concurrent-safe)
		ChatRoom room = findOrCreateGroupRoomForSpot(request.getSpotId());

		// 호출 유저를 멤버로 등록 (이미 있으면 no-op)
		if (currentUserId != null && !currentUserId.isBlank()) {
			ensureMember(room.getId(), currentUserId);
		}

		UserEntity currentUser = findCurrentUser(currentUserId);
		return ChatRoomResponse.from(room, buildEnrichment(room, currentUser));
	}

	/**
	 * 1:1 채팅방을 시작합니다. A↔B 가 이미 있으면 기존 방을 반환 (카카오톡 스타일).
	 */
	public ChatRoomResponse createPersonalRoom(CreatePersonalChatRoomRequest request, String currentUserId) {
		if (currentUserId == null || currentUserId.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		String partnerId = request.getPartnerId();
		if (partnerId == null || partnerId.isBlank()) {
			throw new BusinessException(ErrorCode.CHAT_PERSONAL_REQUIRES_PARTNER);
		}
		if (Objects.equals(currentUserId, partnerId)) {
			throw new BusinessException(ErrorCode.CHAT_PERSONAL_SELF_NOT_ALLOWED);
		}

		UserEntity me = userRepository.findById(currentUserId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		UserEntity partner = userRepository.findById(partnerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_PARTNER_NOT_FOUND));

		// 양방향 차단 검증 — 한쪽이라도 차단 중이면 1:1 시작 불가.
		// 이미 존재하는 방에 다시 접근하는 경우도 막아, 차단 상태에서 새 메시지를 보낼 수 없도록 한다.
		if (chatBlockRepository.existsBetween(me.getId(), partner.getId())) {
			throw new BusinessException(ErrorCode.CHAT_BLOCKED_BETWEEN_USERS);
		}

		ChatRoom room = lookupPersonalRoom(me.getId(), partner.getId());
		if (room == null) {
			try {
				room = chatRoomRepository.save(
					ChatRoom.builder()
						.type(ChatRoomType.PERSONAL)
						.isDeleted(false)
						.build()
				);
				ensureMember(room.getId(), me.getId());
				ensureMember(room.getId(), partner.getId());
			} catch (DataIntegrityViolationException race) {
				// 멤버 INSERT 시 (room_id, user_id) UNIQUE 가 충돌하는 경우는 ensureMember 가
				// 내부에서 흡수하므로 여기까지 오기 어렵다. 안전망으로 재조회.
				room = lookupPersonalRoom(me.getId(), partner.getId());
				if (room == null) {
					throw race;
				}
			}
			// PERSONAL 은 (userA, userB) canonical pair 의 partial unique index 가 없는 한
			// 동시 요청에서 두 방이 만들어질 수 있다 (CodeRabbit 지적, heavy lift). 한 번 더 재조회하여
			// 그 사이 다른 트랜잭션이 만든 방이 있으면 가장 오래된 것을 채택하고 본 트랜잭션이 만든 방은 soft-delete.
			ChatRoom existingNow = lookupPersonalRoom(me.getId(), partner.getId());
			if (existingNow != null && !existingNow.getId().equals(room.getId())) {
				ChatRoom older = existingNow.getId() < room.getId() ? existingNow : room;
				ChatRoom newer = existingNow.getId() < room.getId() ? room : existingNow;
				newer.markDeleted();
				chatRoomRepository.save(newer);
				room = older;
			}
		}

		return ChatRoomResponse.from(room, buildEnrichment(room, me));
	}

	private ChatRoom lookupPersonalRoom(String userA, String userB) {
		return chatRoomMemberRepository.findPersonalRoomIdsBetween(userA, userB).stream()
			.flatMap(id -> chatRoomRepository.findById(id).stream())
			.filter(r -> r.getType() == ChatRoomType.PERSONAL && !r.isDeleted())
			.min(Comparator.comparing(ChatRoom::getId))
			.orElse(null);
	}

	/**
	 * 채팅방 단건 상세 조회. 멤버만 접근 가능.
	 */
	@Transactional(readOnly = true)
	public ChatRoomResponse getRoom(Long roomId) {
		return getRoom(roomId, null);
	}

	@Transactional(readOnly = true)
	public ChatRoomResponse getRoom(Long roomId, String currentUserId) {
		ChatRoom room = findRoomOrThrow(roomId);
		assertMembership(roomId, currentUserId);
		UserEntity currentUser = findCurrentUser(currentUserId);
		return ChatRoomResponse.from(room, buildEnrichment(room, currentUser));
	}

	/**
	 * 스팟 ID로 연결된 채팅방을 조회합니다. 멤버 방만 반환.
	 */
	@Transactional(readOnly = true)
	public List<ChatRoomResponse> getRoomsBySpot(String spotId) {
		return getRoomsBySpot(spotId, null);
	}

	@Transactional(readOnly = true)
	public List<ChatRoomResponse> getRoomsBySpot(String spotId, String currentUserId) {
		UserEntity currentUser = findCurrentUser(currentUserId);
		List<ChatRoom> rooms = chatRoomRepository.findBySpotId(spotId);
		if (currentUser != null) {
			Set<Long> myRoomIds = Set.copyOf(chatRoomMemberRepository.findChatRoomIdsByUserId(currentUser.getId()));
			rooms = rooms.stream()
				.filter(r -> !r.isDeleted())
				.filter(r -> myRoomIds.contains(r.getId()))
				.toList();
		} else {
			rooms = List.of();
		}
		Map<Long, ChatRoomEnrichment> enrichments = buildEnrichments(rooms, currentUser);
		return rooms.stream()
			.map(room -> ChatRoomResponse.from(room, enrichments.getOrDefault(room.getId(), ChatRoomEnrichment.empty())))
			.toList();
	}

	/**
	 * 유저 ID로 참여 중인 채팅방 목록을 조회합니다. 본인 멤버십과 교집합.
	 */
	@Transactional(readOnly = true)
	public List<ChatRoomResponse> getRoomsByUser(String userId) {
		return getRoomsByUser(userId, null);
	}

	@Transactional(readOnly = true)
	public List<ChatRoomResponse> getRoomsByUser(String userId, String currentUserId) {
		UserEntity currentUser = findCurrentUser(currentUserId);
		List<Long> targetRoomIds = chatRoomMemberRepository.findChatRoomIdsByUserId(userId);
		if (currentUser != null) {
			Set<Long> myRoomIds = Set.copyOf(chatRoomMemberRepository.findChatRoomIdsByUserId(currentUser.getId()));
			targetRoomIds = targetRoomIds.stream().filter(myRoomIds::contains).toList();
		} else {
			targetRoomIds = List.of();
		}
		List<ChatRoom> rooms = targetRoomIds.isEmpty()
			? List.of()
			: chatRoomRepository.findAllById(targetRoomIds).stream()
				.filter(r -> !r.isDeleted())
				.toList();
		Map<Long, ChatRoomEnrichment> enrichments = buildEnrichments(rooms, currentUser);
		return rooms.stream()
			.map(room -> ChatRoomResponse.from(room, enrichments.getOrDefault(room.getId(), ChatRoomEnrichment.empty())))
			.toList();
	}

	// ─────────────────────────────────────────────
	// 멤버십 (Membership) — 외부 도메인 (Spot) 에서도 호출 가능
	// ─────────────────────────────────────────────

	/**
	 * 채팅방에 유저를 멤버로 추가합니다. 이미 멤버면 no-op.
	 * SpotService 의 matchSpot 등에서 자동 가입 시 호출.
	 */
	public void ensureMember(Long roomId, String userId) {
		if (userId == null || userId.isBlank()) {
			return;
		}
		if (chatRoomMemberRepository.existsByChatRoomIdAndUserId(roomId, userId)) {
			return;
		}
		try {
			chatRoomMemberRepository.save(
				ChatRoomMember.builder()
					.chatRoomId(roomId)
					.userId(userId)
					.build()
			);
		} catch (DataIntegrityViolationException race) {
			// (room_id, user_id) UNIQUE 위반 — 다른 트랜잭션이 동일 멤버를 먼저 등록함.
			// idempotent 한 작업이므로 무시.
		}
	}

	/**
	 * 스팟 GROUP 방을 idempotent 하게 보장 + 참가자 일괄 가입.
	 * SpotService 측에서 매칭 완료 시 호출.
	 */
	/**
	 * 채팅방에서 호출자를 멤버에서 제거한다.
	 *
	 * <p>동작:
	 * <ul>
	 *   <li>멤버 row 삭제 (이미 없으면 멱등 no-op).</li>
	 *   <li>GROUP 방이면 {@code "OO 님이 나갔습니다."} SYSTEM 메시지를 저장 후 SSE broadcast.
	 *       PERSONAL 방은 시스템 메시지 없이 조용히 나간다 (1:1 특성상 상대만 인지하면 됨).</li>
	 *   <li>나간 후 멤버가 0 명이면 방을 soft-delete (is_deleted=true) — GROUP/PERSONAL 동일.</li>
	 * </ul>
	 *
	 * <p>이미 멤버가 아니면 {@link ErrorCode#CHAT_ROOM_ACCESS_DENIED} — 비멤버가 leave 시도하면
	 * 다른 멤버에게 잘못된 SYSTEM 메시지가 발사되는 것을 막는다.
	 */
	public void leaveRoom(Long roomId, String currentUserId) {
		if (currentUserId == null || currentUserId.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		ChatRoom room = findRoomOrThrow(roomId);

		// delete 반환값으로 멤버십 검증 — assertMembership 선행 후 delete 하는 패턴은
		// concurrent leave 시 두 요청이 모두 assert 를 통과해 SYSTEM 메시지를 이중 발사함.
		long deleted = chatRoomMemberRepository.deleteByChatRoomIdAndUserId(roomId, currentUserId);
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
		}

		if (room.getType() == ChatRoomType.GROUP) {
			broadcastLeaveSystemMessage(room, currentUserId);
		}

		// 빈 방 GC
		if (chatRoomMemberRepository.countByChatRoomId(roomId) == 0) {
			room.markDeleted();
			chatRoomRepository.save(room);
		}
	}

	/**
	 * SYSTEM 타입의 "OO 님이 나갔습니다" 메시지를 저장하고 트랜잭션 커밋 이후 SSE broadcast.
	 * 커밋 이후 발사는 sendMessage 와 동일한 phantom 방지 규약.
	 */
	private void broadcastLeaveSystemMessage(ChatRoom room, String userId) {
		String nickname = userRepository.findById(userId)
			.map(UserEntity::getNickname)
			.orElse("알 수 없는 사용자");
		ChatMessage saved = chatMessageRepository.save(
			ChatMessage.builder()
				.chatRoomId(room.getId())
				.senderId(ChatMessage.SYSTEM_SENDER_ID)
				.type(ChatMessageType.SYSTEM)
				.content(nickname + "님이 나갔습니다.")
				.build()
		);
		ChatMessageResponse payload = ChatMessageResponse.from(saved);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				sseEmitterService.broadcast(room.getId(), payload);
			}
		});
	}

	public ChatRoom ensureGroupRoomForSpot(String spotId, Collection<String> participantUserIds) {
		ChatRoom room = findOrCreateGroupRoomForSpot(spotId);
		if (participantUserIds != null) {
			for (String userId : participantUserIds) {
				ensureMember(room.getId(), userId);
			}
		}
		return room;
	}

	/**
	 * Feed(Post) 단계에서 GROUP 채팅방을 미리 생성합니다.
	 * postId로 방을 추적하며, Spot 전환 전까지 spotId는 null로 유지됩니다.
	 * 이미 방이 있으면 idempotent하게 멤버만 추가합니다.
	 */
	public ChatRoom ensureGroupRoomForPost(String postId, Collection<String> memberUserIds) {
		ChatRoom room = chatRoomRepository
			.findFirstByPostIdAndTypeAndIsDeletedFalse(postId, ChatRoomType.GROUP)
			.orElseGet(() -> {
				try {
					return chatRoomRepository.save(
						ChatRoom.builder()
							.postId(postId)
							.type(ChatRoomType.GROUP)
							.isDeleted(false)
							.build()
					);
				} catch (DataIntegrityViolationException race) {
					// 동시 요청이 먼저 생성 — partial unique index (post_id, type) WHERE is_deleted=false 가 막음.
					return chatRoomRepository
						.findFirstByPostIdAndTypeAndIsDeletedFalse(postId, ChatRoomType.GROUP)
						.orElseThrow(() -> race);
				}
			});
		if (memberUserIds != null) {
			for (String userId : memberUserIds) {
				ensureMember(room.getId(), userId);
			}
		}
		return room;
	}

	/**
	 * Feed → Spot 전환 시 기존 postId 기반 GROUP 방의 spotId를 연결합니다.
	 * 채팅 내역과 기존 멤버를 그대로 유지하면서 Spot 맥락으로 승격됩니다.
	 * postId 기반 방이 없으면 spotId로 신규 GROUP 방을 생성합니다.
	 */
	public ChatRoom linkGroupRoomToSpot(String postId, String spotId, Collection<String> allMemberIds) {
		ChatRoom room = chatRoomRepository
			.findFirstByPostIdAndTypeAndIsDeletedFalse(postId, ChatRoomType.GROUP)
			.map(existing -> {
				existing.linkSpot(spotId);
				return chatRoomRepository.save(existing);
			})
			.orElseGet(() -> findOrCreateGroupRoomForSpot(spotId));
		if (allMemberIds != null) {
			for (String userId : allMemberIds) {
				ensureMember(room.getId(), userId);
			}
		}
		return room;
	}

	/**
	 * spot 의 GROUP 방을 조회 또는 생성. partial unique index
	 * (spot_id WHERE type=GROUP AND is_deleted=false) 와 짝이 되어 동시 요청에서도
	 * 정확히 1 개의 방만 살아남도록 보장한다.
	 */
	private ChatRoom findOrCreateGroupRoomForSpot(String spotId) {
		return chatRoomRepository
			.findFirstBySpotIdAndTypeAndIsDeletedFalse(spotId, ChatRoomType.GROUP)
			.orElseGet(() -> {
				try {
					return chatRoomRepository.save(
						ChatRoom.builder()
							.spotId(spotId)
							.type(ChatRoomType.GROUP)
							.isDeleted(false)
							.build()
					);
				} catch (DataIntegrityViolationException race) {
					// 다른 트랜잭션이 먼저 만들었음 — partial unique index 가 두 번째 insert 를 막음.
					// 재조회하여 idempotent 한 결과를 돌려준다.
					return chatRoomRepository
						.findFirstBySpotIdAndTypeAndIsDeletedFalse(spotId, ChatRoomType.GROUP)
						.orElseThrow(() -> race);
				}
			});
	}

	// ─────────────────────────────────────────────
	// 메시지 (Message)
	// ─────────────────────────────────────────────

	/**
	 * 채팅방의 메시지를 커서 기반 페이지네이션으로 조회합니다. 멤버만 가능.
	 */
	@Transactional(readOnly = true)
	public ChatMessageListResponse.Result getMessages(Long roomId, Long cursor, int size) {
		return getMessages(roomId, cursor, size, null);
	}

	@Transactional(readOnly = true)
	public ChatMessageListResponse.Result getMessages(Long roomId, Long cursor, int size, String currentUserId) {
		ChatRoom room = findRoomOrThrow(roomId);
		assertMembership(roomId, currentUserId);

		PageRequest pageRequest = PageRequest.of(0, size + 1);
		List<ChatMessage> messages = cursor == null
			? chatMessageRepository.findByChatRoomIdOrderByIdDesc(roomId, pageRequest)
			: chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, cursor, pageRequest);

		// 발신자 닉네임 배치 조회 — FRONTEND.md 계약: authorName 필드 필요
		Set<String> senderIds = messages.stream()
			.map(ChatMessage::getSenderId)
			.filter(id -> id != null && !ChatMessage.SYSTEM_SENDER_ID.equals(id))
			.collect(Collectors.toSet());
		Map<String, String> nicknameById = senderIds.isEmpty()
			? Map.of()
			: userRepository.findAllById(senderIds).stream()
				.collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));

		// 차단 매핑 — PERSONAL 방에서만 적용
		Map<String, LocalDateTime> blockedSinceBySenderId = resolveBlockedSinceForRoom(room, currentUserId);

		List<ChatMessageResponse> responses = messages.stream()
			.map(m -> ChatMessageResponse.from(
				m,
				nicknameById.get(m.getSenderId()),
				isMessageBlocked(m, blockedSinceBySenderId)
			))
			.toList();

		return ChatMessageListResponse.of(responses, size);
	}

	/**
	 * PERSONAL 방에서 본인이 차단한 발신자별 차단 시점 매핑.
	 * GROUP 방이거나 비인증 호출이면 빈 맵 반환 → 차단 필터 자동 비활성.
	 */
	private Map<String, LocalDateTime> resolveBlockedSinceForRoom(ChatRoom room, String currentUserId) {
		if (room.getType() != ChatRoomType.PERSONAL
			|| currentUserId == null || currentUserId.isBlank()) {
			return Map.of();
		}
		return chatBlockRepository.findByBlockerIdOrderByCreatedAtDesc(currentUserId).stream()
			.collect(Collectors.toMap(ChatBlock::getBlockedId, ChatBlock::getCreatedAt));
	}

	/** 메시지 발신자가 차단되어 있고, 메시지가 차단 시점 이후에 작성되었는지. */
	private boolean isMessageBlocked(ChatMessage message, Map<String, LocalDateTime> blockedSinceBySenderId) {
		if (blockedSinceBySenderId.isEmpty()) {
			return false;
		}
		LocalDateTime blockedSince = blockedSinceBySenderId.get(message.getSenderId());
		return blockedSince != null && blockedSince.isBefore(message.getCreatedAt());
	}

	/**
	 * 메시지를 전송합니다. 멤버만 가능. senderId 는 인증된 유저 ID 를 사용합니다.
	 *
	 * <p>트랜잭션 커밋 완료 후 SSE 브로드캐스트를 실행하여
	 * DB 미커밋 상태의 메시지가 클라이언트에 전달되는 phantom message 를 방지합니다.
	 */
	public ChatMessageResponse sendMessage(Long roomId, SendMessageRequest request) {
		return sendMessage(roomId, request, null);
	}

	public ChatMessageResponse sendMessage(Long roomId, SendMessageRequest request, String currentUserId) {
		ChatRoom room = findRoomOrThrow(roomId);
		assertMembership(roomId, currentUserId);

		// PERSONAL 방은 메시지 전송 시점에도 차단 검증 — createPersonalRoom 만 막으면
		// 차단 이전에 존재하던 방으로는 계속 메시지를 보낼 수 있는 반쪽짜리 차단이 됨.
		if (room.getType() == ChatRoomType.PERSONAL && currentUserId != null) {
			String partnerId = chatRoomMemberRepository.findByChatRoomId(roomId).stream()
				.map(ChatRoomMember::getUserId)
				.filter(uid -> !Objects.equals(uid, currentUserId))
				.findFirst()
				.orElse(null);
			if (partnerId != null && chatBlockRepository.existsBetween(currentUserId, partnerId)) {
				throw new BusinessException(ErrorCode.CHAT_BLOCKED_BETWEEN_USERS);
			}
		}

		String authorName = currentUserId == null ? null
			: userRepository.findById(currentUserId).map(UserEntity::getNickname).orElse(null);

		ChatMessage message = ChatMessage.builder()
			.chatRoomId(roomId)
			.senderId(currentUserId)
			.content(request.getContent())
			.build();

		ChatMessageResponse response = ChatMessageResponse.from(chatMessageRepository.save(message), authorName, false);

		// 트랜잭션 커밋 이후에 SSE 브로드캐스트 (phantom message 방지)
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				sseEmitterService.broadcast(roomId, response);
			}
		});

		return response;
	}

	/**
	 * 채팅방의 모든 메시지를 읽음 처리합니다. 멤버만 가능.
	 * 멤버십의 {@code lastReadMessageId} 를 방의 최신 메시지 ID 까지 끌어올린다.
	 */
	public void markAsRead(Long roomId) {
		markAsRead(roomId, null);
	}

	public void markAsRead(Long roomId, String currentUserId) {
		if (currentUserId == null || currentUserId.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		findRoomOrThrow(roomId);
		assertMembership(roomId, currentUserId);

		Long latestMessageId = chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId)
			.map(ChatMessage::getId)
			.orElse(null);
		if (latestMessageId == null) {
			// 빈 방 — 굳이 멤버 row 갱신 안 함. 호환을 위해 SSE 만 broadcast.
			sseEmitterService.broadcastRead(roomId, currentUserId);
			return;
		}
		applyReadMarker(roomId, currentUserId, latestMessageId);
	}

	/**
	 * 특정 메시지까지 읽음 처리. 클라이언트가 "여기까지 봤다" 를 명시적으로 신호하는 경로.
	 * 멤버십의 {@code lastReadMessageId} 를 messageId 까지 끌어올린다 (단조 증가).
	 *
	 * @param roomId      대상 방
	 * @param messageId   읽음 처리할 마지막 메시지 ID. 본 방에 속해야 함.
	 * @param currentUserId 인증된 호출자
	 */
	public void markAsReadUpTo(Long roomId, Long messageId, String currentUserId) {
		if (currentUserId == null || currentUserId.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		findRoomOrThrow(roomId);
		assertMembership(roomId, currentUserId);

		ChatMessage message = chatMessageRepository.findById(messageId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));
		if (!message.getChatRoomId().equals(roomId)) {
			throw new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_IN_ROOM);
		}
		applyReadMarker(roomId, currentUserId, messageId);
	}

	/**
	 * 멤버십의 read 커서를 갱신하고 진행된 경우에만 SSE broadcast.
	 * 멱등 — 이미 더 큰 lastReadMessageId 라면 no-op + broadcast 도 skip.
	 */
	private void applyReadMarker(Long roomId, String userId, Long messageId) {
		ChatRoomMember member = chatRoomMemberRepository
			.findByChatRoomIdAndUserId(roomId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));
		boolean advanced = member.markRead(messageId);
		if (!advanced) {
			return;
		}
		// dirty checking 으로 자동 update. 명시적 save 는 trace 가독성 ↑.
		chatRoomMemberRepository.save(member);
		// 트랜잭션 커밋 이후에 SSE broadcast — commit 실패 시 구독자가 DB 미반영 상태의
		// read 영수증을 받는 phantom 을 방지 (sendMessage 와 동일 규약).
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				sseEmitterService.broadcastRead(roomId, userId, messageId);
			}
		});
	}

	// ─────────────────────────────────────────────
	// 내부 헬퍼
	// ─────────────────────────────────────────────

	public ChatRoom findRoomOrThrow(Long roomId) {
		return chatRoomRepository.findById(roomId)
			.filter(r -> !r.isDeleted())
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
	}

	/** Controller 에서 SSE 구독 전 멤버십 선검증 용도로 호출. */
	public void assertMembershipPublic(Long roomId, String currentUserId) {
		assertMembership(roomId, currentUserId);
	}

	private void assertMembership(Long roomId, String currentUserId) {
		if (currentUserId == null || currentUserId.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(roomId, currentUserId)) {
			throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
		}
	}

	private ChatRoomEnrichment buildEnrichment(ChatRoom room, UserEntity currentUser) {
		return buildEnrichments(List.of(room), currentUser)
			.getOrDefault(room.getId(), ChatRoomEnrichment.empty());
	}

	private Map<Long, ChatRoomEnrichment> buildEnrichments(Collection<ChatRoom> rooms, UserEntity currentUser) {
		if (rooms.isEmpty()) {
			return Map.of();
		}

		List<Long> roomIds = rooms.stream()
			.map(ChatRoom::getId)
			.toList();
		Map<Long, ChatMessage> lastMessagesByRoomId = currentUser == null
			? Map.of()
			: chatMessageRepository.findLatestByChatRoomIds(roomIds)
				.stream()
				.collect(Collectors.toMap(ChatMessage::getChatRoomId, Function.identity()));

		Set<String> spotIds = rooms.stream()
			.map(ChatRoom::getSpotId)
			.filter(spotId -> spotId != null && !spotId.isBlank())
			.collect(Collectors.toSet());
		Map<String, Spot> spotsById = spotIds.isEmpty()
			? Map.of()
			: spotRepository.findAllById(spotIds)
				.stream()
				.collect(Collectors.toMap(Spot::getId, Function.identity()));

		// PERSONAL 방 파트너 lookup
		Map<Long, UserEntity> partnerByRoomId = resolvePersonalPartners(rooms, currentUser);

		// 본인 시점 unread 카운트. 비인증/빈 컬렉션 시 0 으로 fallback.
		Map<Long, Long> unreadByRoomId = resolveUnreadCounts(roomIds, currentUser);

		return rooms.stream()
			.collect(Collectors.toMap(
				ChatRoom::getId,
				room -> ChatRoomEnrichment.builder()
					.lastMessage(lastMessagesByRoomId.get(room.getId()))
					.spot(room.getSpotId() == null ? null : spotsById.get(room.getSpotId()))
					.currentUser(currentUser)
					.partner(partnerByRoomId.get(room.getId()))
					.unreadCount(unreadByRoomId.getOrDefault(room.getId(), 0L))
					.build()
			));
	}

	/**
	 * 방 N 개의 unread 카운트를 한 번의 쿼리로 채워 반환.
	 * 본인 멤버십이 없는 방은 자동 제외 (조인 누락) → 응답 DTO 단계에서 0 으로 노출.
	 */
	private Map<Long, Long> resolveUnreadCounts(Collection<Long> roomIds, UserEntity currentUser) {
		if (currentUser == null || roomIds.isEmpty()) {
			return Map.of();
		}
		return chatMessageRepository.countUnreadByUserAndRoomIds(currentUser.getId(), roomIds)
			.stream()
			.collect(Collectors.toMap(
				row -> (Long) row[0],
				row -> (Long) row[1]
			));
	}

	private Map<Long, UserEntity> resolvePersonalPartners(Collection<ChatRoom> rooms, UserEntity currentUser) {
		if (currentUser == null) {
			return Map.of();
		}
		List<Long> personalRoomIds = rooms.stream()
			.filter(r -> r.getType() == ChatRoomType.PERSONAL)
			.map(ChatRoom::getId)
			.toList();
		if (personalRoomIds.isEmpty()) {
			return Map.of();
		}

		// 각 PERSONAL 방의 멤버 중 본인이 아닌 유저 ID 추출
		Map<Long, String> partnerIdByRoomId = personalRoomIds.stream()
			.collect(Collectors.toMap(
				Function.identity(),
				roomId -> chatRoomMemberRepository.findUserIdsByChatRoomId(roomId).stream()
					.filter(uid -> !Objects.equals(uid, currentUser.getId()))
					.findFirst()
					.orElse(null)
			));

		Set<String> partnerIds = partnerIdByRoomId.values().stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		if (partnerIds.isEmpty()) {
			return Map.of();
		}
		Map<String, UserEntity> partnerById = userRepository.findAllById(partnerIds).stream()
			.collect(Collectors.toMap(UserEntity::getId, Function.identity()));

		return partnerIdByRoomId.entrySet().stream()
			.filter(e -> e.getValue() != null && partnerById.containsKey(e.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey, e -> partnerById.get(e.getValue())));
	}

	private UserEntity findCurrentUser(String currentUserId) {
		if (currentUserId == null || currentUserId.isBlank()) {
			return null;
		}
		return userRepository.findById(currentUserId).orElse(null);
	}

	// ─────────────────────────────────────────────
	// 차단 (Block)
	// ─────────────────────────────────────────────

	/**
	 * 본인이 차단한 유저 목록을 최신순으로 반환.
	 * 닉네임은 함께 조회되어 응답에 채워진다 (deleted user 는 null).
	 */
	@Transactional(readOnly = true)
	public List<ChatBlockResponse> getBlocks(String currentUserId) {
		if (currentUserId == null || currentUserId.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		List<ChatBlock> blocks = chatBlockRepository.findByBlockerIdOrderByCreatedAtDesc(currentUserId);
		if (blocks.isEmpty()) {
			return List.of();
		}
		Set<String> blockedIds = blocks.stream().map(ChatBlock::getBlockedId).collect(Collectors.toSet());
		Map<String, UserEntity> usersById = userRepository.findAllById(blockedIds).stream()
			.collect(Collectors.toMap(UserEntity::getId, Function.identity()));
		return blocks.stream()
			.map(b -> ChatBlockResponse.from(b, usersById.get(b.getBlockedId())))
			.toList();
	}

	/**
	 * 유저를 차단. 멱등 — 이미 차단된 상태면 기존 row 를 반환한다.
	 *
	 * <p>차단 효과:
	 * <ul>
	 *   <li>이 호출 이후 보내진 메시지부터 PERSONAL 방에서 placeholder 로 가려진다 (Q2 정책).</li>
	 *   <li>새 PERSONAL 방 시작 시 양방향 검증으로 막힌다 ({@link #createPersonalRoom}).</li>
	 *   <li>GROUP 방 메시지는 영향 없음 (Q5 정책).</li>
	 * </ul>
	 */
	public ChatBlockResponse blockUser(String currentUserId, String targetUserId) {
		if (currentUserId == null || currentUserId.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		if (targetUserId == null || targetUserId.isBlank()) {
			throw new BusinessException(ErrorCode.CHAT_BLOCK_TARGET_NOT_FOUND);
		}
		if (Objects.equals(currentUserId, targetUserId)) {
			throw new BusinessException(ErrorCode.CHAT_SELF_BLOCK_NOT_ALLOWED);
		}

		UserEntity target = userRepository.findById(targetUserId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_BLOCK_TARGET_NOT_FOUND));

		ChatBlock block = chatBlockRepository.findByBlockerIdAndBlockedId(currentUserId, targetUserId)
			.orElseGet(() -> {
				try {
					return chatBlockRepository.save(
						ChatBlock.builder()
							.blockerId(currentUserId)
							.blockedId(targetUserId)
							.build()
					);
				} catch (DataIntegrityViolationException race) {
					// 동시 차단 — UNIQUE (blocker, blocked) 가 잡고, 안전망으로 재조회.
					return chatBlockRepository.findByBlockerIdAndBlockedId(currentUserId, targetUserId)
						.orElseThrow(() -> race);
				}
			});
		return ChatBlockResponse.from(block, target);
	}

	/**
	 * 차단 해제. 차단한 적 없는 유저를 해제해도 멱등 no-op (200 OK).
	 */
	public void unblockUser(String currentUserId, String targetUserId) {
		if (currentUserId == null || currentUserId.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		if (targetUserId == null || targetUserId.isBlank()) {
			return;
		}
		chatBlockRepository.deleteByBlockerIdAndBlockedId(currentUserId, targetUserId);
	}
}
