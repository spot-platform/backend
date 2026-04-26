package backend.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스팟 노트 작성 요청 DTO")
public class CreateNoteRequest {

	@NotBlank
	@Schema(description = "노트 내용")
	private String content;
}
