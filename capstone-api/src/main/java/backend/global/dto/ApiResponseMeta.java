package backend.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "API 응답 페이징 메타 정보")
public class ApiResponseMeta {
	@Schema(description = "현재 페이지 (0부터 시작)", example = "0")
	private int page;

	@Schema(description = "페이지당 데이터 개수", example = "10")
	private int size;

	@Schema(description = "전체 데이터 개수", example = "100")
	private long total;

	@Schema(description = "다음 페이지 존재 여부", example = "true")
	private boolean hasNext;
}
