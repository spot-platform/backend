package backend.feed.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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
@Schema(description = "준비물 DTO")
public class Preparation {

	@JsonProperty("host_provides")
	@Schema(description = "호스트 제공 항목")
	private List<String> hostProvides;

	@JsonProperty("partner_brings")
	@Schema(description = "파트너 준비 항목")
	private List<String> partnerBrings;

	@JsonProperty("weather_contingency")
	@Schema(description = "날씨 대안")
	private String weatherContingency;

	@JsonProperty("safety_notes")
	@Schema(description = "안전 유의사항")
	private List<String> safetyNotes;

	@JsonProperty("host_tip")
	@Schema(description = "호스트 팁")
	private String hostTip;
}

