package backend.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.global.common.response.ApiResponse;
import backend.user.dto.request.DeleteUserRequest;
import backend.user.dto.request.EmailExistRequest;
import backend.user.dto.request.JoinRequest;
import backend.user.dto.request.PasswordChangeRequest;
import backend.user.dto.request.UpdateProfileRequest;
import backend.user.dto.response.UserResponseDTO;
import backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "유저 프로필 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@Operation(summary = "이메일 중복 확인")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200", description = "이메일 사용 가능 여부 반환"
		)
	})
	@PostMapping("/exist")
	public ApiResponse<Boolean> checkEmailExists(
		@Valid @RequestBody EmailExistRequest request
	) {
		return ApiResponse.success(userService.checkEmailExists(request.email()));
	}

	@Operation(
		summary = "회원가입",
		description = "이메일, 비밀번호, 닉네임으로 자체 계정을 생성합니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200", description = "회원가입 성공"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "409", description = "이미 존재하는 이메일"
		)
	})
	@PostMapping
	public ApiResponse<Void> join(@Valid @RequestBody JoinRequest request) {
		userService.join(request);
		return ApiResponse.success();
	}

	@Operation(
		summary = "내 프로필 조회",
		description = "로그인한 유저의 프로필 정보를 반환합니다. Authorization 헤더 필요."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200", description = "성공"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401", description = "인증 필요"
		)
	})
	@GetMapping("/me")
	public ApiResponse<UserResponseDTO> getMyProfile() {
		String email = getCurrentEmail();
		return ApiResponse.success(userService.getMyProfile(email));
	}

	@Operation(
		summary = "내 프로필 수정",
		description = "nickname, email, phone, avatarUrl 중 변경할 필드만 전송합니다. 수정된 UserResponseDTO 전체를 반환합니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200", description = "수정 성공"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401", description = "인증 필요"
		)
	})
	@PatchMapping("/me")
	public ApiResponse<UserResponseDTO> updateProfile(
		@Valid @RequestBody UpdateProfileRequest request
	) {
		String email = getCurrentEmail();
		return ApiResponse.success(userService.updateProfile(email, request));
	}

	@Operation(
		summary = "비밀번호 변경",
		description = "자체 로그인 유저만 사용 가능. 소셜 로그인 유저는 403 반환. 성공 시 204 No Content."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "204", description = "변경 성공"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400", description = "비밀번호 불일치"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "403", description = "소셜 유저 불가"
		)
	})
	@PatchMapping("/me/password")
	public ResponseEntity<Void> changePassword(
		@Valid @RequestBody PasswordChangeRequest request
	) {
		String email = getCurrentEmail();
		userService.changePassword(email, request);
		return ResponseEntity.noContent().build();
	}

	@Operation(
		summary = "회원탈퇴",
		description = "소프트 딜리트 처리. is_deleted = true로 변경."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200", description = "탈퇴 성공"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400", description = "비밀번호 불일치"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401", description = "인증 필요"
		)
	})
	@DeleteMapping("/me")
	public ApiResponse<Void> deleteUser(@RequestBody(required = false) DeleteUserRequest request) {
		String email = getCurrentEmail();
		userService.deleteUser(email, request != null ? request : new DeleteUserRequest(null));
		return ApiResponse.success();
	}

	private String getCurrentEmail() {
		return SecurityContextHolder.getContext().getAuthentication().getName();
	}
}
