package backend.feed.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import backend.feed.dto.FeedApplicationResponse;
import backend.feed.dto.FeedApplyRequest;
import backend.feed.dto.FeedItemResponse;
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

	@Operation(summary = "피드 목록 조회")
	@GetMapping
	public ApiResponse<FeedListResponse> getFeedItems(FeedListQuery query) {
		return ApiResponse.success(feedItemService.getFeedItems(query));
	}

	@Operation(summary = "피드 상세 조회")
	@GetMapping("/{feedId}")
	public ApiResponse<FeedItemResponse> getFeedItem(@PathVariable String feedId) {
		return ApiResponse.success(feedItemService.getFeedItem(feedId));
	}

	@Operation(summary = "피드 삭제 (소프트 딜리트)")
	@DeleteMapping("/{feedId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteFeedItem(@PathVariable String feedId) {
		feedItemService.deleteFeedItem(feedId);
	}

	@Operation(summary = "피드 신청")
	@PostMapping("/{feedId}/apply")
	public ApiResponse<FeedApplicationResponse> applyToFeed(
			@PathVariable String feedId,
			@RequestBody FeedApplyRequest request) {
		// 추후 인증 도입 시 실제 userId, nickname으로 교체
		FeedApplicationResponse response = feedItemService.applyToFeed(
				feedId, "dummy-user-id", "황호찬", request);
		return ApiResponse.success(response);
	}

	@Operation(summary = "피드 신청 취소")
	@DeleteMapping("/{feedId}/apply")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancelApplication(@PathVariable String feedId) {
		feedItemService.cancelApplication(feedId, "dummy-user-id");
	}

	@Operation(summary = "신청 수락 (작성자 전용) — 펀딩 목표 달성 시 Spot 자동 생성")
	@PatchMapping("/{feedId}/applications/{applicationId}/accept")
	public ApiResponse<FeedApplicationResponse> acceptApplication(
			@PathVariable String feedId,
			@PathVariable String applicationId) {
		FeedApplicationResponse response = feedItemService.acceptApplication(
				feedId, applicationId, "dummy-user-id");
		return ApiResponse.success(response);
	}

	@Operation(summary = "신청 거절 (작성자 전용)")
	@PatchMapping("/{feedId}/applications/{applicationId}/reject")
	public ApiResponse<FeedApplicationResponse> rejectApplication(
			@PathVariable String feedId,
			@PathVariable String applicationId) {
		FeedApplicationResponse response = feedItemService.rejectApplication(
				feedId, applicationId, "dummy-user-id");
		return ApiResponse.success(response);
	}
}
