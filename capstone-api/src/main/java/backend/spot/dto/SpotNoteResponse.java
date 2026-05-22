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

	@Schema(description = "스팟 ID", example = "1")
	private Long spotId;

	@Schema(description = "작성자 닉네임", example = "홍길동")
	private String authorNickname;

	@Schema(description = "노트 내용")
	private String content;

	@Schema(description = "작성 일시")
	private LocalDateTime createdAt;

	public static SpotNoteResponse from(SpotNote note) {
		return SpotNoteResponse.builder()
			.id(note.getId())
			.spotId(note.getSpotId())
			.authorNickname(null)
			.content(note.getContent())
			.createdAt(note.getCreatedAt())
			.build();
	}

	public static SpotNoteResponse of(SpotNote note, String authorNickname) {
		return SpotNoteResponse.builder()
			.id(note.getId())
			.spotId(note.getSpotId())
			.authorNickname(authorNickname)
			.content(note.getContent())
			.createdAt(note.getCreatedAt())
			.build();
	}
}
