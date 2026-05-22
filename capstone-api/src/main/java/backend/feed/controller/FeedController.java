package backend.feed.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import backend.feed.dto.CreateOfferFeedRequest;
import backend.feed.dto.CreateRequestFeedRequest;
import backend.feed.dto.FeedApplicationResponse;
import backend.feed.dto.FeedApplyRequest;
import backend.feed.dto.FeedCreateResponse;
import backend.feed.dto.FeedDetailResponse;
import backend.feed.dto.FeedListQuery;
import backend.feed.dto.FeedListResponse;
import backend.feed.service.FeedItemService;
import backend.global.common.response.ApiResponse;
import backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Feed API", description = "피드 관련 API")
@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController {

	private final FeedItemService feedItemService;

	@Operation(summary = "Offer 피드 등록")
	@PostMapping("/offer")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<FeedCreateResponse> createOfferFeed(
			@RequestBody CreateOfferFeedRequest request,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		return ApiResponse.success(feedItemService.createOfferFeed(
				request, requireAuth(userDetails), userDetails.getNickname()));
	}

	@Operation(summary = "Request 피드 등록")
	@PostMapping("/request")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<FeedCreateResponse> createRequestFeed(
			@RequestBody CreateRequestFeedRequest request,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		return ApiResponse.success(feedItemService.createRequestFeed(
				request, requireAuth(userDetails), userDetails.getNickname()));
	}

	@Operation(summary = "피드 목록 조회")
	@GetMapping
	public ApiResponse<FeedListResponse> getFeedItems(FeedListQuery query) {
		return ApiResponse.success(feedItemService.getFeedItems(query));
	}

	@Operation(summary = "피드 상세 조회")
	@GetMapping("/{feedId}")
	public ApiResponse<FeedDetailResponse> getFeedItem(@PathVariable String feedId) {
		return ApiResponse.success(feedItemService.getFeedItem(feedId));
	}

	@Operation(summary = "피드 삭제 (소프트 딜리트)")
	@DeleteMapping("/{feedId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteFeedItem(
			@PathVariable String feedId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		feedItemService.deleteFeedItem(feedId, requireAuth(userDetails));
	}

	@Operation(summary = "피드 신청")
	@PostMapping("/{feedId}/applications")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<FeedApplicationResponse> applyToFeed(
			@PathVariable String feedId,
			@RequestBody FeedApplyRequest request,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		FeedApplicationResponse response = feedItemService.applyToFeed(
				feedId, requireAuth(userDetails), userDetails.getNickname(), request);
		return ApiResponse.success(response);
	}

	@Operation(summary = "피드 신청 취소")
	@DeleteMapping("/{feedId}/applications/me")
	public ApiResponse<Map<String, String>> cancelApplication(
			@PathVariable String feedId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		feedItemService.cancelApplication(feedId, requireAuth(userDetails));
		return ApiResponse.success(Map.of(
				"feedId", feedId,
				"status", "CANCELLED"
		));
	}

	@Operation(summary = "피드 북마크 추가")
	@PostMapping("/{feedId}/bookmark")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void addBookmark(
			@PathVariable String feedId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		feedItemService.addBookmark(feedId, requireAuth(userDetails));
	}

	@Operation(summary = "피드 북마크 제거")
	@DeleteMapping("/{feedId}/bookmark")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeBookmark(
			@PathVariable String feedId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		feedItemService.removeBookmark(feedId, requireAuth(userDetails));
	}

	@Operation(summary = "신청 수락 (작성자 전용) — 펀딩 목표 달성 시 Spot 자동 생성")
	@PatchMapping("/{feedId}/applications/{applicationId}/accept")
	public ApiResponse<FeedApplicationResponse> acceptApplication(
			@PathVariable String feedId,
			@PathVariable String applicationId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		FeedApplicationResponse response = feedItemService.acceptApplication(
				feedId, applicationId, requireAuth(userDetails));
		return ApiResponse.success(response);
	}

	@Operation(summary = "신청 거절 (작성자 전용)")
	@PatchMapping("/{feedId}/applications/{applicationId}/reject")
	public ApiResponse<FeedApplicationResponse> rejectApplication(
			@PathVariable String feedId,
			@PathVariable String applicationId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		FeedApplicationResponse response = feedItemService.rejectApplication(
				feedId, applicationId, requireAuth(userDetails));
		return ApiResponse.success(response);
	}

	private String requireAuth(CustomUserDetails userDetails) {
		if (userDetails == null || userDetails.getUserId() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
		}
		return userDetails.getUserId();
	}
}
