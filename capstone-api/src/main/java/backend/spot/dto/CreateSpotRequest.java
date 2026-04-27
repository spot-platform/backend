package backend.spot.dto;

import backend.global.enums.PostType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "스팟 생성 요청 DTO")
public class CreateSpotRequest {

	@NotNull
	@Schema(description = "스팟 타입 (OFFER: 제공, REQUEST: 요청)", example = "OFFER",
		requiredMode = Schema.RequiredMode.REQUIRED)
	private PostType type;

	@NotBlank
	@Schema(description = "스팟 제목", example = "한강 자리 제공합니다",
		requiredMode = Schema.RequiredMode.REQUIRED)
	private String title;

	@NotBlank
	@Schema(description = "스팟 상세 설명", example = "여의도 한강공원 명당 자리입니다. 잔디밭 근처 테이블 자리예요.",
		requiredMode = Schema.RequiredMode.REQUIRED)
	private String description;

	@NotNull
	@Schema(description = "포인트 비용", example = "5000",
		requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer pointCost;
}
