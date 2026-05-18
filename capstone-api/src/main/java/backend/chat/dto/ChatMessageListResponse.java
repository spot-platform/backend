package backend.chat.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FRONTEND.md 계약:
 * GET /chat/rooms/{roomId}/messages
 *   response: { data: ChatMessage[], meta: { nextCursor?: string, hasNext: boolean } }
 *
 * ApiResponse.success(messages, meta) 형태로 반환하므로 이 DTO 는 meta 전용.
 * 메시지 배열 자체는 List<ChatMessageResponse> 로 직접 ApiResponse.data 에 들어간다.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "채팅 메시지 목록 커서 메타")
public class ChatMessageListResponse {

	@Schema(description = "다음 페이지 조회용 커서 (마지막 메시지 ID 문자열, 더 없으면 null)", example = "\"42\"")
	private String nextCursor;

	@Schema(description = "이전 메시지가 더 있는지 여부")
	private boolean hasNext;

	/**
	 * size+1 개 조회 결과에서 hasNext 판단 후 meta 생성.
	 * 실제 메시지 목록(result)은 별도로 반환.
	 *
	 * @param messages      service 에서 size+1 개 조회한 전체 결과
	 * @param requestedSize 클라이언트 요청 크기
	 * @return [trimmedMessages, meta] 형태로 쌍 반환
	 */
	public static Result of(List<ChatMessageResponse> messages, int requestedSize) {
		boolean hasNext = messages.size() > requestedSize;
		List<ChatMessageResponse> data = hasNext
			? messages.subList(0, requestedSize)
			: messages;
		String nextCursor = hasNext
			? String.valueOf(data.get(data.size() - 1).getId())
			: null;

		ChatMessageListResponse meta = ChatMessageListResponse.builder()
			.nextCursor(nextCursor)
			.hasNext(hasNext)
			.build();

		return new Result(data, meta);
	}

	public record Result(List<ChatMessageResponse> data, ChatMessageListResponse meta) {
	}
}
