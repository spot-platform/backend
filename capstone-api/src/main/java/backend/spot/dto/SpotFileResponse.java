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

	@Schema(description = "업로더 ID", example = "user-uuid-string")
	private String uploaderId;

	@Schema(description = "파일명", example = "map.png")
	private String fileName;

	@Schema(description = "파일 URL", example = "https://cdn.example.com/files/map.png")
	private String fileUrl;

	@Schema(description = "업로드 일시")
	private LocalDateTime uploadedAt;

	public static SpotFileResponse from(SpotFile file) {
		return SpotFileResponse.builder()
			.id(file.getId())
			.uploaderId(file.getUploaderId())
			.fileName(file.getFileName())
			.fileUrl(file.getFileUrl())
			.uploadedAt(file.getUploadedAt())
			.build();
	}
}
