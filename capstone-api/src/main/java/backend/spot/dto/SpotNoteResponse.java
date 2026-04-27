package backend.spot.dto;

import java.time.LocalDateTime;

import backend.spot.entity.SpotNote;
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
@Schema(description = "스팟 노트 응답 DTO")
public class SpotNoteResponse {

	@Schema(description = "노트 ID", example = "1")
	private Long id;

	@Schema(description = "작성자 ID", example = "user-uuid-string")
	private String authorId;

	@Schema(description = "노트 내용")
	private String content;

	@Schema(description = "작성 일시")
	private LocalDateTime createdAt;

	public static SpotNoteResponse from(SpotNote note) {
		return SpotNoteResponse.builder()
			.id(note.getId())
			.authorId(note.getAuthorId())
			.content(note.getContent())
			.createdAt(note.getCreatedAt())
			.build();
	}
}
