package backend.feed.dto;

import backend.global.enums.AddOnMechanism;
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
@Schema(description = "추가 옵션 DTO")
public class AddOn {

	@Schema(description = "옵션명", example = "추가 재료")
	private String name;

	@Schema(description = "가격", example = "3000")
	private Integer price;

	@Schema(description = "과금 방식", example = "FIXED")
	private AddOnMechanism mechanism;

	@Schema(description = "비고")
	private String note;
}
