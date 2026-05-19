package backend.spot.dto;

import java.time.LocalDateTime;

import backend.spot.entity.SpotFile;
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
@Schema(description = "스팟 파일 응답 DTO")
public class SpotFileResponse {

	@Schema(description = "파일 ID", example = "1")
	private Long id;

	@Schema(description = "스팟 ID", example = "spot-uuid-string")
	private String spotId;

	@Schema(description = "업로더 닉네임", example = "홍길동")
	private String uploaderNickname;

	@Schema(description = "파일명", example = "map.png")
	private String name;

	@Schema(description = "파일 URL", example = "https://cdn.example.com/files/map.png")
	private String url;

	@Schema(description = "파일 크기 (bytes)", nullable = true)
	private Long sizeBytes;

	@Schema(description = "업로드 일시")
	private LocalDateTime uploadedAt;

	public static SpotFileResponse from(SpotFile file) {
		return SpotFileResponse.builder()
			.id(file.getId())
			.spotId(file.getSpotId())
			.uploaderNickname(null)
			.name(file.getFileName())
			.url(file.getFileUrl())
			.sizeBytes(file.getSizeBytes())
			.uploadedAt(file.getUploadedAt())
			.build();
	}

	public static SpotFileResponse of(SpotFile file, String uploaderNickname) {
		return SpotFileResponse.builder()
			.id(file.getId())
			.spotId(file.getSpotId())
			.uploaderNickname(uploaderNickname)
			.name(file.getFileName())
			.url(file.getFileUrl())
			.sizeBytes(file.getSizeBytes())
			.uploadedAt(file.getUploadedAt())
			.build();
	}
}
