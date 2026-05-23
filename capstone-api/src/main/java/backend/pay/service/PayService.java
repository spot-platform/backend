package backend.pay.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import backend.pay.dto.PointBalanceResponse;
import backend.pay.dto.PointTransactionResponse;
import backend.pay.entity.PointTransaction;
import backend.pay.entity.PointTransactionType;
import backend.pay.repository.PointTransactionRepository;
import backend.user.entity.UserEntity;
import backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PayService {

	private final UserRepository userRepository;
	private final PointTransactionRepository pointTransactionRepository;

	/** 포인트 잔액 조회 */
	public PointBalanceResponse getBalance(String userId) {
		UserEntity user = findActiveUser(userId);
		return PointBalanceResponse.builder()
			.balance(user.getPointBalance())
			.updatedAt(formatIso(user.getUpdatedAt()))
			.build();
	}

	/**
	 * 포인트 거래 내역 조회 (최신순, 페이지네이션).
	 * page는 1-base (>=1), size는 1~100. 범위 밖이면 INVALID_INPUT_VALUE.
	 * 컨트롤러에서 ApiResponseMeta로 변환해 응답 meta에 포함.
	 */
	public Page<PointTransactionResponse> getHistory(String userId, int page, int size) {
		findActiveUser(userId);
		if (page < 1 || size < 1 || size > 100) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		PageRequest pageRequest = PageRequest.of(page - 1, size);
		return pointTransactionRepository
			.findByUserIdOrderByCreatedAtDesc(userId, pageRequest)
			.map(PointTransactionResponse::from);
	}

	/** 포인트 충전 (Mock — 실제 PG 미연동) */
	@Transactional
	public PointBalanceResponse charge(String userId, long amount) {
		if (amount < 1000) {
			throw new BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT);
		}
		UserEntity user = findActiveUser(userId);
		user.chargePoint(amount);

		pointTransactionRepository.save(PointTransaction.builder()
			.userId(userId)
			.type(PointTransactionType.CHARGE)
			.amount(amount)
			.balanceAfter(user.getPointBalance())
			.description("포인트 충전")
			.build());

		// user.getUpdatedAt()은 @LastModifiedDate 기준 flush/commit 후에야 갱신됨.
		// 응답에는 현재 시각을 직접 채워 충전 직후의 상태를 반영한다.
		return PointBalanceResponse.builder()
			.balance(user.getPointBalance())
			.updatedAt(formatIso(LocalDateTime.now()))
			.build();
	}

	private UserEntity findActiveUser(String userId) {
		UserEntity user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		if (user.getIsDeleted()) {
			throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
		}
		return user;
	}

	private String formatIso(LocalDateTime dateTime) {
		return dateTime == null ? null : dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}
}
