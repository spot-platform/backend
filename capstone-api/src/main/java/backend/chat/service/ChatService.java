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
import backend.chat.dto.ChatMemberResponse;
import backend.chat.dto.ChatMessageListResponse;
import backend.chat.dto.ChatMessageResponse;
import backend.chat.dto.ChatNotificationSettingResponse;
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

	@Transactional(readOnly = true)
	public List<ChatRoomResponse> getRooms(String currentUserId) {
		return getRooms(currentUserId, null);
	}

	/**
	 * 본인이 멤버인 채팅방 목록. type 필터 가능 (PERSONAL / GROUP).
	 * updatedAt DESC 정렬 — 마지막 메시지 기준 최신 방이 위에 표시된다.
	 */
	@Transactional(readOnly = true)
	public List<ChatRoomResponse> getRooms(String currentUserId, ChatRoomType typeFilter) {
		UserEntity currentUser = findCurrentUser(currentUserId);
		List<ChatRoom> rooms;
		if (currentUser == null) {
			rooms = List.of();
		} else {
			List<Long> roomIds = chatRoomMemberRepository.findChatRoomIdsByUserId(currentUser.getId());
			rooms = roomIds.isEmpty()
				? List.of()
				: chatRoomRepository.findAllById(roomIds).stream()
					.filter(r -> !r.isDeleted())
					.filter(r -> typeFilter == null || r.getType() == typeFilter)
					.sorted(Comparator.comparing(ChatRoom::getUpdatedAt,
						Comparator.nullsLast(Comparator.reverseOrder())))
					.toList();
		}
		Map<Long, ChatRoomEnrichment> enrichments = buildEnrichments(rooms, currentUser);
		return rooms.stream()
			.map(room -> ChatRoomResponse.from(
				room, enrichments.getOrDefault(room.getId(), ChatRoomEnrichment.empty())))
			.toList();
	}

	/**
	 * GROUP 채팅방 생성 (PERSONAL 은 createPersonalRoom 사용).
	 * 동일 spotId 의 GROUP 방이 이미 있으면 idempotent 하게 기존 방을 반환한다.
	 */
	public ChatRoomResponse createRoom(CreateChatRoomRequest request, String currentUserId) {
		if (request.getType() == ChatRoomType.PERSONAL) {
			throw new BusinessException(ErrorCode.CHAT_PERSONAL_REQUIRES_PARTNER);
		}
		if (request.getType() == ChatRoomType.GROUP && request.getSpotId() == null) {
			throw new BusinessException(ErrorCode.GROUP_CHAT_REQUIRES_SPOT);
		}
		if (request.getType() == ChatRoomType.GROUP
				&& !spotRepository.existsById(parseSpotId(request.getSpotId()))) {
			throw new BusinessException(ErrorCode.SPOT_NOT_FOUND);
		}

		ChatRoom room = findOrCreateGroupRoomForSpot(request.getSpotId(), null);

		if (currentUserId != null && !currentUserId.isBlank()) {
			ensureMember(room.getId(), currentUserId);
		}

		UserEntity currentUser = findCurrentUser(currentUserId);
		return ChatRoomResponse.from(room, buildEnrichment(room, currentUser));
	}

	/**
	 * 1:1 채팅방 시작. A↔B 가 이미 있으면 기존 방을 반환 (카카오톡 스타일).
	 * canonicalPair 기반 DB 유니크 인덱스로 동시 생성 race 를 방지한다.
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

		if (chatBlockRepository.existsBetween(me.getId(), partner.getId())) {
			throw new BusinessException(ErrorCode.CHAT_BLOCKED_BETWEEN_USERS);
		}

		String canonicalPair = ChatRoom.buildCanonicalPair(me.getId(), partner.getId());

		ChatRoom room = chatRoomRepository
			.findFirstByCanonicalPairAndTypeAndIsDeletedFalse(canonicalPair, ChatRoomType.PERSONAL)
			.orElseGet(() -> {
				try {
					ChatRoom created = chatRoomRepository.save(
						ChatRoom.builder()
							.type(ChatRoomType.PERSONAL)
							.canonicalPair(canonicalPair)
							.isDeleted(false)
							.build()
					);
					ensureMember(created.getId(), me.getId());
					ensureMember(created.getId(), partner.getId());
					return created;
				} catch (DataIntegrityViolationException race) {
					// canonical_pair partial unique index 충돌 — 다른 트랜잭션이 먼저 생성.
					return chatRoomRepository
						.findFirstByCanonicalPairAndTypeAndIsDeletedFalse(canonicalPair, ChatRoomType.PERSONAL)
						.orElseThrow(() -> race);
				}
			});

		// 기존 방 반환 경우에도 멤버십 보장 (멱등 upsert)
		ensureMember(room.getId(), me.getId());
		ensureMember(room.getId(), partner.getId());

		return ChatRoomResponse.from(room, buildEnrichment(room, me));
	}

	@Transactional(readOnly = true)
	public ChatRoomResponse getRoom(Long roomId, String currentUserId) {
		ChatRoom room = findRoomOrThrow(roomId);
		assertMembership(roomId, currentUserId);
		UserEntity currentUser = findCurrentUser(currentUserId);
		return ChatRoomResponse.from(room, buildEnrichment(room, currentUser));
	}

	@Transactional(readOnly = true)
	public List<ChatRoomResponse> getRoomsByFeed(String feedId, String currentUserId) {
		UserEntity currentUser = findCurrentUser(currentUserId);
		List<ChatRoom> rooms = chatRoomRepository.findByPostIdAndIsDeletedFalse(feedId);
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
			.map(room -> ChatRoomResponse.from(
				room, enrichments.getOrDefault(room.getId(), ChatRoomEnrichment.empty())))
			.toList();
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
			.map(room -> ChatRoomResponse.from(
				room, enrichments.getOrDefault(room.getId(), ChatRoomEnrichment.empty())))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<ChatMemberResponse> getMembers(Long roomId, String currentUserId) {
		assertMembership(roomId, currentUserId);
		List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
		List<String> userIds = members.stream().map(ChatRoomMember::getUserId).toList();
		Map<String, UserEntity> usersById = userRepository.findAllById(userIds).stream()
			.collect(Collectors.toMap(UserEntity::getId, Function.identity()));
		return members.stream()
			.map(m -> ChatMemberResponse.from(m, usersById.get(m.getUserId())))
			.toList();
	}

	// ─────────────────────────────────────────────
	// 멤버십 (Membership) — 외부 도메인에서도 호출 가능
	// ─────────────────────────────────────────────

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
			// (room_id, user_id) UNIQUE — 다른 트랜잭션이 먼저 등록. 무시.
		}
	}

	public void leaveRoom(Long roomId, String currentUserId) {
		if (currentUserId == null || currentUserId.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		ChatRoom room = findRoomOrThrow(roomId);

		long deleted = chatRoomMemberRepository.deleteByChatRoomIdAndUserId(roomId, currentUserId);
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
		}

		if (room.getType() == ChatRoomType.GROUP) {
			broadcastSystemMessage(room, resolveNickname(currentUserId) + "님이 나갔습니다.");
		}

		if (chatRoomMemberRepository.countByChatRoomId(roomId) == 0) {
			room.markDeleted();
			chatRoomRepository.save(room);
		}
	}

	// ─────────────────────────────────────────────
	// 방 알림 설정 (Notification mute)
	// ─────────────────────────────────────────────

	/** 현재 사용자의 이 방에 대한 알림 수신 설정을 조회한다. */
	@Transactional(readOnly = true)
	public ChatNotificationSettingResponse getRoomNotification(Long roomId, String currentUserId) {
		ChatRoomMember member = findMemberOrThrow(roomId, currentUserId);
		return ChatNotificationSettingResponse.of(member.isNotificationEnabled());
	}

	/** 현재 사용자의 이 방에 대한 알림 수신 설정을 변경한다. (음소거 on/off) */
	public ChatNotificationSettingResponse updateRoomNotification(Long roomId, String currentUserId, boolean enabled) {
		ChatRoomMember member = findMemberOrThrow(roomId, currentUserId);
		member.updateNotificationEnabled(enabled);
		return ChatNotificationSettingResponse.of(enabled);
	}

	/**
	 * 룸 스코프 알림 발송 대상 — 알림을 켜둔(음소거하지 않은) 멤버의 userId 목록.
	 * 투표 시작/마감 등 룸 알림 발송 시 muted 멤버를 제외하기 위해 사용한다.
	 */
	@Transactional(readOnly = true)
	public List<String> getNotifiableUserIds(Long roomId) {
		return chatRoomMemberRepository.findByChatRoomId(roomId).stream()
			.filter(ChatRoomMember::isNotificationEnabled)
			.map(ChatRoomMember::getUserId)
			.toList();
	}

	private ChatRoomMember findMemberOrThrow(Long roomId, String currentUserId) {
		if (currentUserId == null || currentUserId.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		return chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, currentUserId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));
	}

	public ChatRoom ensureGroupRoomForSpot(String spotId, Collection<String> participantUserIds) {
		ChatRoom room = findOrCreateGroupRoomForSpot(spotId, null);
		if (participantUserIds != null) {
			for (String userId : participantUserIds) {
				ensureMember(room.getId(), userId);
			}
		}
		return room;
	}

	/**
	 * Feed 생성 시 GROUP 채팅방을 미리 만든다.
	 * postId 로 방을 추적하며 Spot 전환 전까지 spotId 는 null.
	 * 이미 방이 있으면 idempotent 하게 멤버만 추가.
	 *
	 * @param title       Feed 제목 — 방 이름으로 설정.
	 */
	public ChatRoom ensureGroupRoomForPost(String postId, String title, Collection<String> memberUserIds) {
		ChatRoom room = chatRoomRepository
			.findFirstByPostIdAndTypeAndIsDeletedFalse(postId, ChatRoomType.GROUP)
			.orElseGet(() -> {
				try {
					return chatRoomRepository.save(
						ChatRoom.builder()
							.postId(postId)
							.name(title)
							.type(ChatRoomType.GROUP)
							.isDeleted(false)
							.build()
					);
				} catch (DataIntegrityViolationException race) {
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
	 * Feed → Spot 전환 시 기존 postId 기반 GROUP 방의 spotId 를 연결하고 이름을 갱신한다.
	 * 채팅 내역과 기존 멤버는 그대로 유지된다.
	 *
	 * @param spotTitle Spot 제목 — 방 이름을 피드 제목에서 스팟 제목으로 업데이트.
	 */
	public ChatRoom linkGroupRoomToSpot(String postId, String spotId, String spotTitle,
			Collection<String> allMemberIds) {
		ChatRoom room = chatRoomRepository
			.findFirstByPostIdAndTypeAndIsDeletedFalse(postId, ChatRoomType.GROUP)
			.map(existing -> {
				existing.linkSpot(spotId);
				if (spotTitle != null && !spotTitle.isBlank()) {
					existing.updateName(spotTitle);
				}
				return chatRoomRepository.save(existing);
			})
			.orElseGet(() -> findOrCreateGroupRoomForSpot(spotId, spotTitle));
		if (allMemberIds != null) {
			for (String userId : allMemberIds) {
				ensureMember(room.getId(), userId);
			}
		}
		return room;
	}

	/**
	 * 스팟 취소/완료 시 채팅방을 읽기 전용으로 전환하고 SYSTEM 메시지를 발송한다.
	 * spotId 에 연결된 GROUP 방이 없으면 no-op.
	 *
	 * @param spotId        대상 스팟 ID
	 * @param systemMessage "스팟이 완료되었습니다." 등
	 */
	public void closeGroupRoom(String spotId, String systemMessage) {
		chatRoomRepository.findFirstBySpotIdAndTypeAndIsDeletedFalse(spotId, ChatRoomType.GROUP)
			.ifPresent(room -> {
				room.markReadOnly();
				chatRoomRepository.save(room);
				broadcastSystemMessage(room, systemMessage);
			});
	}

	private ChatRoom findOrCreateGroupRoomForSpot(String spotId, String name) {
		return chatRoomRepository
			.findFirstBySpotIdAndTypeAndIsDeletedFalse(spotId, ChatRoomType.GROUP)
			.orElseGet(() -> {
				try {
					return chatRoomRepository.save(
						ChatRoom.builder()
							.spotId(spotId)
							.name(name)
							.type(ChatRoomType.GROUP)
							.isDeleted(false)
							.build()
					);
				} catch (DataIntegrityViolationException race) {
					return chatRoomRepository
						.findFirstBySpotIdAndTypeAndIsDeletedFalse(spotId, ChatRoomType.GROUP)
						.orElseThrow(() -> race);
				}
			});
	}

	// ─────────────────────────────────────────────
	// 메시지 (Message)
	// ─────────────────────────────────────────────

	@Transactional(readOnly = true)
	public ChatMessageListResponse.Result getMessages(Long roomId, Long cursor, int size, String currentUserId) {
		ChatRoom room = findRoomOrThrow(roomId);
		assertMembership(roomId, currentUserId);

		PageRequest pageRequest = PageRequest.of(0, size + 1);
		List<ChatMessage> messages = cursor == null
			? chatMessageRepository.findByChatRoomIdOrderByIdDesc(roomId, pageRequest)
			: chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, cursor, pageRequest);

		Set<String> senderIds = messages.stream()
			.map(ChatMessage::getSenderId)
			.filter(id -> id != null && !ChatMessage.SYSTEM_SENDER_ID.equals(id))
			.collect(Collectors.toSet());
		Map<String, String> nicknameById = senderIds.isEmpty()
			? Map.of()
			: userRepository.findAllById(senderIds).stream()
				.collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));

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
	 * 사진 모인 탭 — 방에 공유된 IMAGE 타입 메시지를 최신순으로 조회한다.
	 * 차단한 사용자가 올린 이미지는 blocked 처리되어 내려간다.
	 */
	@Transactional(readOnly = true)
	public List<ChatMessageResponse> getPhotos(Long roomId, String currentUserId) {
		ChatRoom room = findRoomOrThrow(roomId);
		assertMembership(roomId, currentUserId);

		List<ChatMessage> images = chatMessageRepository
			.findByChatRoomIdAndTypeOrderByIdDesc(roomId, ChatMessageType.IMAGE);

		Set<String> senderIds = images.stream()
			.map(ChatMessage::getSenderId)
			.filter(id -> id != null && !ChatMessage.SYSTEM_SENDER_ID.equals(id))
			.collect(Collectors.toSet());
		Map<String, String> nicknameById = senderIds.isEmpty()
			? Map.of()
			: userRepository.findAllById(senderIds).stream()
				.collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));

		Map<String, LocalDateTime> blockedSinceBySenderId = resolveBlockedSinceForRoom(room, currentUserId);

		return images.stream()
			.map(m -> ChatMessageResponse.from(
				m,
				nicknameById.get(m.getSenderId()),
				isMessageBlocked(m, blockedSinceBySenderId)
			))
			.toList();
	}

	/**
	 * 메시지 전송.
	 *
	 * <ul>
	 *   <li>읽기 전용 방에는 전송 불가 (스팟 완료/취소 후).</li>
	 *   <li>PERSONAL 방 차단 검증 — 양방향 (차단한 쪽/당한 쪽 모두 전송 불가).</li>
	 *   <li>IMAGE / FILE 타입의 경우 fileUrl 필수.</li>
	 *   <li>커밋 후 SSE 브로드캐스트 (phantom 방지) + 멤버 배지 갱신.</li>
	 * </ul>
	 */
	public ChatMessageResponse sendMessage(Long roomId, SendMessageRequest request, String currentUserId) {
		ChatRoom room = findRoomOrThrow(roomId);
		assertMembership(roomId, currentUserId);

		if (room.isReadOnly()) {
			throw new BusinessException(ErrorCode.CHAT_ROOM_READ_ONLY);
		}

		// PERSONAL 방 양방향 차단 검증
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

		ChatMessageType type = request.getType() != null ? request.getType() : ChatMessageType.USER;

		if ((type == ChatMessageType.IMAGE || type == ChatMessageType.FILE)
			&& (request.getFileUrl() == null || request.getFileUrl().isBlank())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		if (type == ChatMessageType.FILE
			&& (request.getFileName() == null || request.getFileSizeBytes() == null)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}

		String authorName = currentUserId == null ? null
			: userRepository.findById(currentUserId).map(UserEntity::getNickname).orElse(null);

		ChatMessage message = ChatMessage.builder()
			.chatRoomId(roomId)
			.senderId(currentUserId)
			.type(type)
			.content(request.getContent())
			.fileUrl(request.getFileUrl())
			.fileName(request.getFileName())
			.fileSizeBytes(request.getFileSizeBytes())
			.build();

		ChatMessageResponse response = ChatMessageResponse.from(chatMessageRepository.save(message), authorName, false);

		// updatedAt 갱신 — 채팅 목록 최신순 정렬 기준
		room.touch();
		chatRoomRepository.save(room);

		// 커밋 후 SSE 브로드캐스트
		List<String> memberUserIds = chatRoomMemberRepository.findUserIdsByChatRoomId(roomId);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				sseEmitterService.broadcast(roomId, response);
				// 배지 갱신: 방 멤버 전원에게 unread count 재계산 없이 +1 이벤트 전달
				// (정확한 count 는 DB 쿼리 비용 발생 → 클라이언트가 증분 처리)
				for (String uid : memberUserIds) {
					if (!Objects.equals(uid, currentUserId)) {
						sseEmitterService.broadcastBadgeUpdate(uid, roomId, -1L);
					}
				}
			}
		});

		return response;
	}

	/** 타이핑 이벤트 전달. 멤버 검증 포함. DB 저장 없음. */
	public void broadcastTyping(Long roomId, String currentUserId) {
		assertMembership(roomId, currentUserId);
		sseEmitterService.broadcastTyping(roomId, currentUserId);
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
			sseEmitterService.broadcastRead(roomId, currentUserId);
			return;
		}
		applyReadMarker(roomId, currentUserId, latestMessageId);
	}

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

	private void applyReadMarker(Long roomId, String userId, Long messageId) {
		ChatRoomMember member = chatRoomMemberRepository
			.findByChatRoomIdAndUserId(roomId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));
		boolean advanced = member.markRead(messageId);
		if (!advanced) {
			return;
		}
		chatRoomMemberRepository.save(member);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				sseEmitterService.broadcastRead(roomId, userId, messageId);
			}
		});
	}

	// ─────────────────────────────────────────────
	// 차단 (Block)
	// ─────────────────────────────────────────────

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
					return chatBlockRepository.findByBlockerIdAndBlockedId(currentUserId, targetUserId)
						.orElseThrow(() -> race);
				}
			});
		return ChatBlockResponse.from(block, target);
	}

	public void unblockUser(String currentUserId, String targetUserId) {
		if (currentUserId == null || currentUserId.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		if (targetUserId == null || targetUserId.isBlank()) {
			return;
		}
		chatBlockRepository.deleteByBlockerIdAndBlockedId(currentUserId, targetUserId);
	}

	// ─────────────────────────────────────────────
	// 내부 헬퍼
	// ─────────────────────────────────────────────

	public ChatRoom findRoomOrThrow(Long roomId) {
		return chatRoomRepository.findById(roomId)
			.filter(r -> !r.isDeleted())
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
	}

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

	private void broadcastSystemMessage(ChatRoom room, String content) {
		room.touch();
		chatRoomRepository.save(room);
		ChatMessage saved = chatMessageRepository.save(
			ChatMessage.builder()
				.chatRoomId(room.getId())
				.senderId(ChatMessage.SYSTEM_SENDER_ID)
				.type(ChatMessageType.SYSTEM)
				.content(content)
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

	private Map<String, LocalDateTime> resolveBlockedSinceForRoom(ChatRoom room, String currentUserId) {
		if (room.getType() != ChatRoomType.PERSONAL
			|| currentUserId == null || currentUserId.isBlank()) {
			return Map.of();
		}
		return chatBlockRepository.findByBlockerIdOrderByCreatedAtDesc(currentUserId).stream()
			.collect(Collectors.toMap(ChatBlock::getBlockedId, ChatBlock::getCreatedAt));
	}

	private boolean isMessageBlocked(ChatMessage message, Map<String, LocalDateTime> blockedSinceBySenderId) {
		if (blockedSinceBySenderId.isEmpty()) {
			return false;
		}
		// 차단한 유저의 메시지는 차단 시점과 무관하게 모두 가린다 (카카오톡 동작과 동일).
		return blockedSinceBySenderId.containsKey(message.getSenderId());
	}

	private ChatRoomEnrichment buildEnrichment(ChatRoom room, UserEntity currentUser) {
		return buildEnrichments(List.of(room), currentUser)
			.getOrDefault(room.getId(), ChatRoomEnrichment.empty());
	}

	private Map<Long, ChatRoomEnrichment> buildEnrichments(Collection<ChatRoom> rooms, UserEntity currentUser) {
		if (rooms.isEmpty()) {
			return Map.of();
		}

		List<Long> roomIds = rooms.stream().map(ChatRoom::getId).toList();
		Map<Long, ChatMessage> lastMessagesByRoomId = currentUser == null
			? Map.of()
			: chatMessageRepository.findLatestByChatRoomIds(roomIds).stream()
				.collect(Collectors.toMap(ChatMessage::getChatRoomId, Function.identity()));

		Set<Long> spotIds = rooms.stream()
			.map(room -> parseSpotId(room.getSpotId()))
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		Map<Long, Spot> spotsById = spotIds.isEmpty()
			? Map.of()
			: spotRepository.findAllById(spotIds).stream()
				.collect(Collectors.toMap(Spot::getId, Function.identity()));

		Map<Long, UserEntity> partnerByRoomId = resolvePersonalPartners(rooms, currentUser);
		Map<Long, Long> unreadByRoomId = resolveUnreadCounts(roomIds, currentUser);

		return rooms.stream()
			.collect(Collectors.toMap(
				ChatRoom::getId,
				room -> {
					Long parsedSpotId = parseSpotId(room.getSpotId());
					return ChatRoomEnrichment.builder()
						.lastMessage(lastMessagesByRoomId.get(room.getId()))
						.spot(parsedSpotId != null ? spotsById.get(parsedSpotId) : null)
						.currentUser(currentUser)
						.partner(partnerByRoomId.get(room.getId()))
						.unreadCount(unreadByRoomId.getOrDefault(room.getId(), 0L))
						.build();
				}
			));
	}

	private Map<Long, Long> resolveUnreadCounts(Collection<Long> roomIds, UserEntity currentUser) {
		if (currentUser == null || roomIds.isEmpty()) {
			return Map.of();
		}
		return chatMessageRepository.countUnreadByUserAndRoomIds(currentUser.getId(), roomIds).stream()
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

	private String resolveNickname(String userId) {
		return userRepository.findById(userId)
			.map(UserEntity::getNickname)
			.orElse("알 수 없는 사용자");
	}

	/** String spotId → Long 안전 파싱. UUID 형태의 레거시 값이나 null 이면 null 반환. */
	private Long parseSpotId(String spotId) {
		if (spotId == null || spotId.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(spotId);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
