package backend.feed.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.feed.dto.FeedListQuery;
import backend.feed.dto.FeedListResponse;
import backend.feed.service.FeedItemService;
import backend.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Feed API", description = "피드 관련 API")
@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

	private final FeedItemService feedItemService;

	@Operation(summary = "피드 목록 조회", description = "탭, 타입, 상태, 카테고리 등 필터링 조건에 따라 피드 아이템 목록을 조회합니다.")
	@GetMapping
	public ApiResponse<FeedListResponse> getFeedItems(FeedListQuery query) {
		FeedListResponse response = feedItemService.getFeedItems(query);
		return ApiResponse.success(response);
	}

	@Operation(summary = "피드 상세 조회", description = "ID를 통해 피드 아이템 상세 정보를 조회합니다.")
	@GetMapping("/{feedId}")
	public ApiResponse<Object> getFeedItem(@PathVariable String feedId) {
		return ApiResponse.success(null);
	}
}
