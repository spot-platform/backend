package backend.feed.dto;

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
@Schema(description = "포함 항목 DTO")
public class IncludedItem {

	@Schema(description = "항목명", example = "재료")
	private String name;

	@Schema(description = "값/설명", example = "쌀 2kg")
	private String value;
}
