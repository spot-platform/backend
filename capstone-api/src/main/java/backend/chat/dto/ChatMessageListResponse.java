package backend.chat.dto;

import java.util.List;

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
@Schema(description = "채팅 메시지 목록 응답 DTO (커서 기반 페이지네이션)")
public class ChatMessageListResponse {

	@Schema(description = "메시지 목록 (최신순 → 오래된 순으로 정렬하여 렌더링)")
	private List<ChatMessageResponse> messages;

	@Schema(description = "다음 페이지 조회용 커서 (마지막 메시지 ID, 없으면 null)")
	private Long nextCursor;

	@Schema(description = "이전 메시지가 더 있는지 여부")
	private boolean hasMore;

	public static ChatMessageListResponse of(List<ChatMessageResponse> messages, int requestedSize) {
		boolean hasMore = messages.size() == requestedSize;
		Long nextCursor = hasMore ? messages.get(messages.size() - 1).getId() : null;

		return ChatMessageListResponse.builder()
			.messages(messages)
			.nextCursor(nextCursor)
			.hasMore(hasMore)
			.build();
	}
}
