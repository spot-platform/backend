package backend.post.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.global.common.response.ApiResponse;
import backend.post.dto.CreateOfferPostRequest;
import backend.post.dto.CreateRequestPostRequest;
import backend.post.dto.PostCompletionResponse;
import backend.post.dto.PostResponse;
import backend.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Post API", description = "게시글 관련 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@Operation(summary = "게시글 목록 조회", description = "모든 게시글 목록을 조회합니다.")
	@GetMapping
	public ApiResponse<List<PostResponse>> getPosts() {
		return ApiResponse.success(List.of());
	}

	@Operation(summary = "게시글 상세 조회", description = "ID를 통해 게시글 상세 정보를 조회합니다.")
	@GetMapping("/{postId}")
	public ApiResponse<PostResponse> getPost(@PathVariable String postId) {
		return ApiResponse.success(null);
	}

	@Operation(summary = "Offer 게시글 등록", description = "새로운 Offer(제공) 게시글을 등록합니다.")
	@PostMapping("/offer")
	public ApiResponse<PostCompletionResponse> createOfferPost(@RequestBody CreateOfferPostRequest request) {
		PostCompletionResponse response = postService.createOfferPost(request);
		return ApiResponse.success(response);
	}

	@Operation(summary = "Request 게시글 등록", description = "새로운 Request(요청) 게시글을 등록합니다.")
	@PostMapping("/request")
	public ApiResponse<PostCompletionResponse> createRequestPost(@RequestBody CreateRequestPostRequest request) {
		PostCompletionResponse response = postService.createRequestPost(request);
		return ApiResponse.success(response);
	}
}
