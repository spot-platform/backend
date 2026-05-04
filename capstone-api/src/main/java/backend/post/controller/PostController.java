package backend.post.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

	@Operation(summary = "게시글 상세 조회")
	@GetMapping("/{postId}")
	public ApiResponse<PostResponse> getPost(@PathVariable String postId) {
		return ApiResponse.success(postService.getPost(postId));
	}

	@Operation(summary = "게시글 삭제 (소프트 딜리트)")
	@DeleteMapping("/{postId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePost(@PathVariable String postId) {
		postService.deletePost(postId);
	}

	@Operation(summary = "Offer 게시글 등록")
	@PostMapping("/offer")
	public ApiResponse<PostCompletionResponse> createOfferPost(@RequestBody CreateOfferPostRequest request) {
		return ApiResponse.success(postService.createOfferPost(request));
	}

	@Operation(summary = "Request 게시글 등록")
	@PostMapping("/request")
	public ApiResponse<PostCompletionResponse> createRequestPost(@RequestBody CreateRequestPostRequest request) {
		return ApiResponse.success(postService.createRequestPost(request));
	}
}
