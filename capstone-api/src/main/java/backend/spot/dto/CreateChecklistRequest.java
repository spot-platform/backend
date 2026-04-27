package backend.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스팟 체크리스트 항목 추가 요청 DTO")
public class CreateChecklistRequest {

	@NotBlank
	@Schema(description = "항목 내용", example = "돗자리 준비")
	private String content;
}
