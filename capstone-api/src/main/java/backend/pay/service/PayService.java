package backend.pay.service;

import java.util.List;

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
			.updatedAt(user.getUpdatedAt().toString())
			.build();
	}

	/** 포인트 거래 내역 조회 (최신순, 페이지네이션) */
	public List<PointTransactionResponse> getHistory(String userId, int page, int size) {
		PageRequest pageRequest = PageRequest.of(page - 1, size);
		return pointTransactionRepository
			.findByUserIdOrderByCreatedAtDesc(userId, pageRequest)
			.stream()
			.map(PointTransactionResponse::from)
			.toList();
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

		return PointBalanceResponse.builder()
			.balance(user.getPointBalance())
			.updatedAt(user.getUpdatedAt().toString())
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
}
