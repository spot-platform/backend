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

	/**
	 * N+1 패턴으로 hasMore 를 정확히 판단합니다.
	 * ChatService 는 size+1 개를 조회하고, 여기서 size 개를 초과하면 hasMore=true 로 확정합니다.
	 * 정확히 size 개만 왔을 경우 마지막 페이지로 판단하여 불필요한 round-trip 을 방지합니다.
	 *
	 * @param messages      실제 조회된 메시지 목록 (최대 size+1 개)
	 * @param requestedSize 클라이언트가 요청한 페이지 크기
	 */
	public static ChatMessageListResponse of(List<ChatMessageResponse> messages, int requestedSize) {
		boolean hasMore = messages.size() > requestedSize;

		List<ChatMessageResponse> result = hasMore
			? messages.subList(0, requestedSize)
			: messages;

		Long nextCursor = hasMore ? result.get(result.size() - 1).getId() : null;

		return ChatMessageListResponse.builder()
			.messages(result)
			.nextCursor(nextCursor)
			.hasMore(hasMore)
			.build();
	}
}
