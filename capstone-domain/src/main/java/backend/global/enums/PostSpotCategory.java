package backend.global.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 스팟 카테고리")
public enum PostSpotCategory {
	음식_요리("음식·요리"),
	BBQ_조개("BBQ·조개"),
	공동구매("공동구매"),
	교육("교육"),
	운동("운동"),
	공예("공예"),
	음악("음악"),
	기타("기타");

	private final String label;

	PostSpotCategory(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
