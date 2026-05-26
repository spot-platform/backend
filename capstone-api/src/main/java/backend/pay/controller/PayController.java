package backend.pay.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import backend.global.common.response.ApiResponse;
import backend.global.dto.ApiResponseMeta;
import backend.global.security.CustomUserDetails;
import backend.pay.dto.ChargePointRequest;
import backend.pay.dto.PointBalanceResponse;
import backend.pay.dto.PointTransactionResponse;
import backend.pay.service.PayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Pay", description = "포인트 충전 및 거래 내역 API")
@RestController
@RequestMapping("/api/v1/pay")
@RequiredArgsConstructor
public class PayController {

	private final PayService payService;

	@Operation(summary = "포인트 잔액 조회")
	@GetMapping("/balance")
	public ApiResponse<PointBalanceResponse> getBalance(
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ApiResponse.success(payService.getBalance(requireUserId(userDetails)));
	}

	@Operation(
		summary = "포인트 거래 내역 조회",
		description = "최신순. page는 1-base (>=1), size는 1~100. 응답 meta: { page, size, total, hasNext }."
	)
	@GetMapping("/history")
	public ApiResponse<List<PointTransactionResponse>> getHistory(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		Page<PointTransactionResponse> result =
			payService.getHistory(requireUserId(userDetails), page, size);
		ApiResponseMeta meta = ApiResponseMeta.builder()
			.page(page)
			.size(size)
			.total(result.getTotalElements())
			.hasNext(result.hasNext())
			.build();
		return ApiResponse.success(result.getContent(), meta);
	}

	@Operation(
		summary = "포인트 충전",
		description = "최소 충전 금액 1000. MVP 기준 Mock 충전 (실제 PG 미연동)."
	)
	@PostMapping("/charge")
	public ApiResponse<PointBalanceResponse> charge(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody ChargePointRequest request
	) {
		return ApiResponse.success(payService.charge(requireUserId(userDetails), request.amount()));
	}

	private String requireUserId(CustomUserDetails userDetails) {
		if (userDetails == null || userDetails.getUserId() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
		}
		return userDetails.getUserId();
	}
}
