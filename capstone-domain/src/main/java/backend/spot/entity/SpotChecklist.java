package backend.spot.entity;

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
@Table(name = "spot_checklists")
public class SpotChecklist {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "spot_id", nullable = false)
	private String spotId;

	@Column(nullable = false)
	private String content;

	@Builder.Default
	@Column(nullable = false)
	private Boolean isDone = false;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * 체크리스트 항목의 완료 여부를 토글합니다.
	 */
	public void toggleDone() {
		this.isDone = !this.isDone;
	}

	/**
	 * 체크리스트 항목의 내용을 수정합니다.
	 */
	public void updateContent(String content) {
		this.content = content;
	}
}
