package backend.chat.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import backend.chat.dto.ChatMessageListResponse;
import backend.chat.dto.ChatMessageResponse;
import backend.chat.dto.ChatRoomResponse;
import backend.chat.dto.ChatRoomResponse.ChatRoomEnrichment;
import backend.chat.dto.CreateChatRoomRequest;
import backend.chat.dto.CreatePersonalChatRoomRequest;
import backend.chat.dto.SendMessageRequest;
import backend.chat.entity.ChatMessage;
import backend.chat.entity.ChatRoom;
import backend.chat.entity.ChatRoomMember;
import backend.chat.entity.ChatRoomType;
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
			rooms = roomIds.isEmpty() ? List.of() : chatRoomRepository.findAllById(roomIds);
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

		// 동일 spot 의 활성 GROUP 방 idempotent 재사용
		ChatRoom room = chatRoomRepository
			.findFirstBySpotIdAndTypeAndIsDeletedFalse(request.getSpotId(), ChatRoomType.GROUP)
			.orElseGet(() -> chatRoomRepository.save(
				ChatRoom.builder()
					.spotId(request.getSpotId())
					.type(ChatRoomType.GROUP)
					.isDeleted(false)
					.build()
			));

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

		// 기존 PERSONAL 방 재사용
		List<Long> existing = chatRoomMemberRepository.findPersonalRoomIdsBetween(me.getId(), partner.getId());
		ChatRoom room = existing.stream()
			.flatMap(id -> chatRoomRepository.findById(id).stream())
			.filter(r -> r.getType() == ChatRoomType.PERSONAL && !r.isDeleted())
			.min(Comparator.comparing(ChatRoom::getId))
			.orElse(null);

		if (room == null) {
			room = chatRoomRepository.save(
				ChatRoom.builder()
					.type(ChatRoomType.PERSONAL)
					.isDeleted(false)
					.build()
			);
			ensureMember(room.getId(), me.getId());
			ensureMember(room.getId(), partner.getId());
		}

		return ChatRoomResponse.from(room, buildEnrichment(room, me));
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
			rooms = rooms.stream().filter(r -> myRoomIds.contains(r.getId())).toList();
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
		List<ChatRoom> rooms = targetRoomIds.isEmpty() ? List.of() : chatRoomRepository.findAllById(targetRoomIds);
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
		chatRoomMemberRepository.save(
			ChatRoomMember.builder()
				.chatRoomId(roomId)
				.userId(userId)
				.build()
		);
	}

	/**
	 * 스팟 GROUP 방을 idempotent 하게 보장 + 참가자 일괄 가입.
	 * SpotService 측에서 매칭 완료 시 호출.
	 */
	public ChatRoom ensureGroupRoomForSpot(String spotId, Collection<String> participantUserIds) {
		ChatRoom room = chatRoomRepository
			.findFirstBySpotIdAndTypeAndIsDeletedFalse(spotId, ChatRoomType.GROUP)
			.orElseGet(() -> chatRoomRepository.save(
				ChatRoom.builder()
					.spotId(spotId)
					.type(ChatRoomType.GROUP)
					.isDeleted(false)
					.build()
			));
		if (participantUserIds != null) {
			for (String userId : participantUserIds) {
				ensureMember(room.getId(), userId);
			}
		}
		return room;
	}

	// ─────────────────────────────────────────────
	// 메시지 (Message)
	// ─────────────────────────────────────────────

	/**
	 * 채팅방의 메시지를 커서 기반 페이지네이션으로 조회합니다. 멤버만 가능.
	 */
	@Transactional(readOnly = true)
	public ChatMessageListResponse getMessages(Long roomId, Long cursor, int size) {
		return getMessages(roomId, cursor, size, null);
	}

	@Transactional(readOnly = true)
	public ChatMessageListResponse getMessages(Long roomId, Long cursor, int size, String currentUserId) {
		findRoomOrThrow(roomId);
		assertMembership(roomId, currentUserId);

		PageRequest pageRequest = PageRequest.of(0, size + 1); // +1 로 hasMore 판단
		List<ChatMessage> messages;

		if (cursor == null) {
			messages = chatMessageRepository.findByChatRoomIdOrderByIdDesc(roomId, pageRequest);
		} else {
			messages = chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, cursor, pageRequest);
		}

		List<ChatMessageResponse> responses = messages.stream()
			.map(ChatMessageResponse::from)
			.toList();

		return ChatMessageListResponse.of(responses, size);
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
		findRoomOrThrow(roomId);
		assertMembership(roomId, currentUserId);

		ChatMessage message = ChatMessage.builder()
			.chatRoomId(roomId)
			.senderId(currentUserId)
			.content(request.getContent())
			.build();

		ChatMessageResponse response = ChatMessageResponse.from(chatMessageRepository.save(message));

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
	 * 채팅방의 메시지를 읽음 처리합니다. 멤버만 가능.
	 * TODO: ChatMessageReadStatus 테이블 도입 후 유저별 읽음 상태 저장 구현 (PR B)
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
		sseEmitterService.broadcastRead(roomId, currentUserId);
	}

	// ─────────────────────────────────────────────
	// 내부 헬퍼
	// ─────────────────────────────────────────────

	public ChatRoom findRoomOrThrow(Long roomId) {
		return chatRoomRepository.findById(roomId)
			.filter(r -> !r.isDeleted())
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
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

		return rooms.stream()
			.collect(Collectors.toMap(
				ChatRoom::getId,
				room -> ChatRoomEnrichment.builder()
					.lastMessage(lastMessagesByRoomId.get(room.getId()))
					.spot(room.getSpotId() == null ? null : spotsById.get(room.getSpotId()))
					.currentUser(currentUser)
					.partner(partnerByRoomId.get(room.getId()))
					.build()
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
}
