package backend.chat.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.chat.dto.ChatMessageListResponse;
import backend.chat.dto.ChatMessageResponse;
import backend.chat.dto.ChatRoomResponse;
import backend.chat.dto.CreateChatRoomRequest;
import backend.chat.dto.SendMessageRequest;
import backend.chat.entity.ChatMessage;
import backend.chat.entity.ChatRoom;
import backend.chat.repository.ChatMessageRepository;
import backend.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final SseEmitterService sseEmitterService;

	// ─────────────────────────────────────────────
	// 채팅방 (Room)
	// ─────────────────────────────────────────────

	/**
	 * 전체 채팅방 목록을 조회합니다.
	 * TODO: 인증 도입 후 현재 로그인 유저가 참여한 방만 반환하도록 수정
	 */
	@Transactional(readOnly = true)
	public List<ChatRoomResponse> getRooms() {
		return chatRoomRepository.findAll()
			.stream()
			.map(ChatRoomResponse::from)
			.toList();
	}

	/**
	 * 채팅방을 생성합니다.
	 *
	 * @param request 채팅방 타입 및 스팟 ID (그룹 채팅인 경우)
	 */
	public ChatRoomResponse createRoom(CreateChatRoomRequest request) {
		ChatRoom room = ChatRoom.builder()
			.spotId(request.getSpotId())
			.type(request.getType())
			.build();

		return ChatRoomResponse.from(chatRoomRepository.save(room));
	}

	/**
	 * 채팅방 단건 상세 조회를 합니다.
	 */
	@Transactional(readOnly = true)
	public ChatRoomResponse getRoom(Long roomId) {
		return ChatRoomResponse.from(findRoomOrThrow(roomId));
	}

	/**
	 * 스팟 ID로 연결된 채팅방을 조회합니다.
	 */
	@Transactional(readOnly = true)
	public List<ChatRoomResponse> getRoomsBySpot(String spotId) {
		return chatRoomRepository.findBySpotId(spotId)
			.stream()
			.map(ChatRoomResponse::from)
			.toList();
	}

	/**
	 * 유저 ID로 참여 중인 채팅방 목록을 조회합니다.
	 * TODO: ChatRoomMember 테이블 도입 후 실제 필터링 구현 (현재는 전체 반환)
	 */
	@Transactional(readOnly = true)
	public List<ChatRoomResponse> getRoomsByUser(String userId) {
		return chatRoomRepository.findAll()
			.stream()
			.map(ChatRoomResponse::from)
			.toList();
	}

	// ─────────────────────────────────────────────
	// 메시지 (Message)
	// ─────────────────────────────────────────────

	/**
	 * 채팅방의 메시지를 커서 기반 페이지네이션으로 조회합니다.
	 *
	 * <p>cursor 가 null 이면 최신 메시지부터, 있으면 해당 ID 이전 메시지부터 size 개 반환합니다.
	 * 반환 순서는 ID 내림차순(최신 → 과거)이며 클라이언트에서 뒤집어 렌더링합니다.
	 *
	 * @param roomId 채팅방 ID
	 * @param cursor 마지막으로 받은 메시지 ID (없으면 null)
	 * @param size   한 번에 가져올 메시지 수
	 */
	@Transactional(readOnly = true)
	public ChatMessageListResponse getMessages(Long roomId, Long cursor, int size) {
		findRoomOrThrow(roomId);

		PageRequest pageRequest = PageRequest.of(0, size);
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
	 * 메시지를 전송합니다.
	 * 1. DB에 메시지를 저장합니다.
	 * 2. 해당 채팅방을 구독 중인 모든 SSE 클라이언트에게 실시간으로 브로드캐스트합니다.
	 *
	 * TODO: 인증 도입 후 senderId 를 실제 로그인 유저 ID로 교체
	 *
	 * @param roomId  전송할 채팅방 ID
	 * @param request 메시지 내용
	 */
	public ChatMessageResponse sendMessage(Long roomId, SendMessageRequest request) {
		findRoomOrThrow(roomId);

		ChatMessage message = ChatMessage.builder()
			.chatRoomId(roomId)
			.senderId("dummy-user-id")     // TODO: 실제 인증 유저 ID
			.content(request.getContent())
			.build();

		ChatMessageResponse response = ChatMessageResponse.from(chatMessageRepository.save(message));

		// SSE 브로드캐스트: 해당 채팅방 구독 중인 모든 클라이언트에게 전송
		sseEmitterService.broadcast(roomId, response);

		return response;
	}

	/**
	 * 채팅방의 메시지를 읽음 처리합니다.
	 * TODO: ChatMessageReadStatus 테이블 도입 후 유저별 읽음 상태 저장 구현
	 *
	 * @param roomId 읽음 처리할 채팅방 ID
	 */
	public void markAsRead(Long roomId) {
		findRoomOrThrow(roomId);
		// TODO: 읽음 상태 저장 로직 구현
	}

	// ─────────────────────────────────────────────
	// 내부 헬퍼
	// ─────────────────────────────────────────────

	private ChatRoom findRoomOrThrow(Long roomId) {
		return chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new IllegalArgumentException("해당 채팅방을 찾을 수 없습니다. roomId=" + roomId));
	}
}
