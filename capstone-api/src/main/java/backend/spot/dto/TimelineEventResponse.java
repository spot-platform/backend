package backend.spot.dto;

import java.time.LocalDateTime;

import backend.spot.entity.SpotTimelineEvent;
import backend.spot.entity.TimelineEventKind;
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
@Schema(description = "스팟 타임라인 이벤트 응답 DTO")
public class TimelineEventResponse {

	@Schema(description = "타임라인 이벤트 ID", example = "1")
	private Long id;

	@Schema(description = "이벤트 종류", example = "CREATED")
	private TimelineEventKind kind;

	@Schema(description = "행위자 ID", example = "user-uuid-string")
	private String actorId;

	@Schema(description = "행위자 닉네임", example = "홍길동")
	private String actorNickname;

	@Schema(description = "부가 내용", nullable = true)
	private String content;

	@Schema(description = "발생 일시", example = "2024-04-10T10:00:00")
	private LocalDateTime createdAt;

	public static TimelineEventResponse of(SpotTimelineEvent event, String actorNickname) {
		return TimelineEventResponse.builder()
			.id(event.getId())
			.kind(event.getKind())
			.actorId(event.getActorId())
			.actorNickname(actorNickname)
			.content(event.getContent())
			.createdAt(event.getCreatedAt())
			.build();
	}
}
