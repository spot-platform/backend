package backend.spot.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스팟 일정 후보 슬롯. (spot_id, slot_date, slot_hour) 단위로 유일하며,
 * confirmed=true 인 슬롯이 확정 일정이다. 가용 사용자는 SpotScheduleSlotAvailability 로 분리한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "spot_schedule_slots",
	uniqueConstraints = @UniqueConstraint(columnNames = {"spot_id", "slot_date", "slot_hour"})
)
public class SpotScheduleSlot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "spot_id", nullable = false)
	private String spotId;

	@Column(name = "slot_date", nullable = false)
	private LocalDate slotDate;

	@Column(name = "slot_hour", nullable = false)
	private int slotHour;

	@Builder.Default
	@Column(nullable = false)
	private boolean confirmed = false;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
