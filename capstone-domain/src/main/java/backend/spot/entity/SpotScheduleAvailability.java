package backend.spot.entity;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 특정 일정 슬롯에 대해 "가능"을 표시한 사용자. (slot_id, user_id) 단위로 유일.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
	name = "spot_schedule_availabilities",
	uniqueConstraints = @UniqueConstraint(columnNames = {"slot_id", "user_id"})
)
public class SpotScheduleAvailability {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@Column(columnDefinition = "VARCHAR(36)")
	private String id;

	@Column(name = "slot_id", nullable = false)
	private Long slotId;

	@Column(name = "user_id", nullable = false)
	private String userId;
}
