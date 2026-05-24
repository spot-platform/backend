package backend.spot.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "spot_settlements")
public class SpotSettlement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "spot_id", nullable = false)
	private Long spotId;

	/** 논리 FK — users.id (물리 FK 미설정, CAPSTONE.md §3-1) */
	@Column(nullable = false)
	private String requesterId;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String summary;

	@Column(nullable = false)
	private Integer totalAmount;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(nullable = false)
	private WorkflowApprovalStatus status = WorkflowApprovalStatus.PENDING;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column
	private LocalDateTime approvedAt;

	/**
	 * 정산 요청을 승인 처리합니다. (PENDING → APPROVED)
	 */
	public void approve() {
		if (this.status != WorkflowApprovalStatus.PENDING) {
			throw new IllegalStateException("승인 대기 중인 정산만 승인할 수 있습니다. 현재 상태: " + this.status);
		}
		this.status = WorkflowApprovalStatus.APPROVED;
		this.approvedAt = LocalDateTime.now();
	}
}
