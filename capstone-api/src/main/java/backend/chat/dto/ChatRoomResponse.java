package backend.chat.dto;

import java.time.LocalDateTime;

import backend.chat.entity.ChatMessage;
import backend.chat.entity.ChatRoom;
import backend.chat.entity.ChatRoomType;
import backend.spot.entity.Spot;
import backend.user.entity.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "채팅방 응답 DTO")
public class ChatRoomResponse {

	private static final int LAST_MESSAGE_PREVIEW_MAX_LENGTH = 100;
	private static final String PERSONAL_DEFAULT_TITLE = "대화방";

	@Schema(description = "채팅방 ID", example = "1")
	private Long id;

	@Schema(description = "연결된 스팟 ID (그룹 채팅인 경우)", example = "550e8400-e29b-41d4-a716-446655440000")
	private String spotId;

	@Schema(description = "연결된 포스트 ID (있는 경우)")
	private String postId;

	@Schema(description = "채팅방 타입 (GROUP / PERSONAL)", example = "GROUP")
	private ChatRoomType type;

	@Schema(description = "그룹 채팅방 이름 (직접 지정)")
	private String name;

	@Schema(description = "그룹 채팅방 이미지 URL")
	private String imageUrl;

	@Schema(description = "생성 일시")
	private LocalDateTime createdAt;

	@Schema(description = "마지막 메시지 미리보기", example = "안녕하세요")
	private String lastMessagePreview;

	@Schema(description = "마지막 메시지 전송 일시")
	private LocalDateTime lastMessageAt;

	@Schema(description = "채팅방 목록 표시용 제목", example = "스팟 제목")
	private String title;

	@Schema(description = "채팅방 목록 표시용 부제", example = "OPEN")
	private String subtitle;

	@Schema(description = "현재 사용자 ID", example = "user-uuid-string")
	private String currentUserId;

	@Schema(description = "현재 사용자 이름", example = "홍길동")
	private String currentUserName;

	@Schema(description = "PERSONAL 채팅 상대방 ID")
	private String partnerId;

	@Schema(description = "PERSONAL 채팅 상대방 닉네임")
	private String partnerNickname;

	@Schema(description = "읽지 않은 메시지 수 (PR B 에서 실 카운트 구현)", example = "0")
	private Integer unreadCount;

	public static ChatRoomResponse from(ChatRoom room) {
		return from(room, ChatRoomEnrichment.empty());
	}

	public static ChatRoomResponse from(ChatRoom room, ChatRoomEnrichment enrichment) {
		ChatMessage lastMessage = enrichment.getLastMessage();
		Spot spot = enrichment.getSpot();
		UserEntity currentUser = enrichment.getCurrentUser();
		UserEntity partner = enrichment.getPartner();

		return ChatRoomResponse.builder()
			.id(room.getId())
			.spotId(room.getSpotId())
			.postId(room.getPostId())
			.type(room.getType())
			.name(room.getName())
			.imageUrl(room.getImageUrl())
			.createdAt(room.getCreatedAt())
			.lastMessagePreview(resolveLastMessagePreview(lastMessage))
			.lastMessageAt(lastMessage == null ? null : lastMessage.getCreatedAt())
			.title(resolveTitle(room, spot, partner))
			.subtitle(resolveSubtitle(room, spot, partner))
			.currentUserId(currentUser == null ? null : currentUser.getId())
			.currentUserName(currentUser == null ? null : currentUser.getNickname())
			.partnerId(partner == null ? null : partner.getId())
			.partnerNickname(partner == null ? null : partner.getNickname())
			// TODO: per-user unread tracking — PR B 에서 실 구현
			.unreadCount(0)
			.build();
	}

	private static String resolveLastMessagePreview(ChatMessage lastMessage) {
		if (lastMessage == null || lastMessage.getContent() == null) {
			return null;
		}
		String content = lastMessage.getContent();
		if (content.length() <= LAST_MESSAGE_PREVIEW_MAX_LENGTH) {
			return content;
		}
		return content.substring(0, LAST_MESSAGE_PREVIEW_MAX_LENGTH);
	}

	private static String resolveTitle(ChatRoom room, Spot spot, UserEntity partner) {
		if (room.getType() == ChatRoomType.PERSONAL) {
			if (partner != null && partner.getNickname() != null) {
				return partner.getNickname();
			}
			return PERSONAL_DEFAULT_TITLE;
		}
		// GROUP: 직접 지정한 name 우선, 없으면 spot 제목
		if (room.getName() != null && !room.getName().isBlank()) {
			return room.getName();
		}
		return spot == null ? null : spot.getTitle();
	}

	private static String resolveSubtitle(ChatRoom room, Spot spot, UserEntity partner) {
		if (room.getType() == ChatRoomType.PERSONAL) {
			return "";
		}
		return spot == null ? null : spot.getStatus().name();
	}

	@Getter
	@Builder
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class ChatRoomEnrichment {

		private ChatMessage lastMessage;
		private Spot spot;
		private UserEntity currentUser;
		private UserEntity partner;

		public static ChatRoomEnrichment empty() {
			return ChatRoomEnrichment.builder().build();
		}
	}
}
