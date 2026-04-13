package backend.spot.dto;

import java.util.List;

import backend.global.dto.ApiResponseMeta;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "스팟 목록 응답 DTO")
public class SpotListResponse {

	@Schema(description = "스팟 목록")
	private List<SpotResponse> data;

	@Schema(description = "페이징 정보")
	private ApiResponseMeta meta;
}
