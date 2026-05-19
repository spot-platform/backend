package backend.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스팟 파일 업로드 요청 DTO")
public class UploadFileRequest {

	@NotBlank
	@Schema(description = "파일명", example = "map.png")
	private String fileName;

	@NotBlank
	@Schema(description = "파일 URL (업로드 후 CDN URL)", example = "https://cdn.example.com/files/map.png")
	private String fileUrl;

	@PositiveOrZero
	@Schema(description = "파일 크기 (bytes)", example = "204800", nullable = true)
	private Long sizeBytes;
}
