package backend.chat.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import backend.chat.dto.ChatMessageListResponse;
import backend.chat.dto.ChatMessageResponse;
import backend.chat.dto.ChatRoomResponse;
import backend.chat.dto.CreateChatRoomRequest;
import backend.chat.dto.SendMessageRequest;
import backend.chat.entity.ChatMessage;
import backend.chat.entity.ChatRoom;
import backend.chat.entity.ChatRoomType;
import backend.chat.repository.ChatMessageRepository;
import backend.chat.repository.ChatRoomRepository;
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
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
	 * GROUP 타입은 반드시 spotId 가 있어야 합니다.
	 */
	public ChatRoomResponse createRoom(CreateChatRoomRequest request) {
		if (request.getType() == ChatRoomType.GROUP && request.getSpotId() == null) {
			throw new BusinessException(ErrorCode.GROUP_CHAT_REQUIRES_SPOT);
		}

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
	 * TODO: ChatRoomMember 테이블 도입 후 실제 필터링 구현
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
	 * <p>실제 size+1 개를 조회하여 다음 페이지 존재 여부(hasMore)를 정확히 판단합니다.
	 * 반환 목록은 요청한 size 개로 잘려 반환됩니다.
	 *
	 * @param roomId 채팅방 ID
	 * @param cursor 마지막으로 받은 메시지 ID (없으면 null, 최신부터 조회)
	 * @param size   한 번에 가져올 메시지 수
	 */
	@Transactional(readOnly = true)
	public ChatMessageListResponse getMessages(Long roomId, Long cursor, int size) {
		findRoomOrThrow(roomId);

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
	 * 메시지를 전송합니다.
	 *
	 * <p>트랜잭션 커밋 완료 후 SSE 브로드캐스트를 실행하여
	 * DB 미커밋 상태의 메시지가 클라이언트에 전달되는 phantom message 를 방지합니다.
	 *
	 * TODO: 인증 도입 후 senderId 를 실제 로그인 유저 ID로 교체
	 */
	public ChatMessageResponse sendMessage(Long roomId, SendMessageRequest request) {
		findRoomOrThrow(roomId);

		ChatMessage message = ChatMessage.builder()
			.chatRoomId(roomId)
			.senderId("dummy-user-id")
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
	 * 채팅방의 메시지를 읽음 처리합니다.
	 * TODO: ChatMessageReadStatus 테이블 도입 후 유저별 읽음 상태 저장 구현
	 */
	public void markAsRead(Long roomId) {
		findRoomOrThrow(roomId);
	}

	// ─────────────────────────────────────────────
	// 내부 헬퍼
	// ─────────────────────────────────────────────

	public ChatRoom findRoomOrThrow(Long roomId) {
		return chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
	}
}
