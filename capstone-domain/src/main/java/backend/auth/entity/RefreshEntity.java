package backend.auth.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
	name = "jwt_refresh_tokens",
	indexes = {
		@Index(name = "idx_refresh_email", columnList = "email"),
		@Index(name = "idx_refresh_created_at", columnList = "createdAt")
	},
	uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token", columnNames = "refresh")
)
public class RefreshEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "VARCHAR(36)")
	private String id;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false, unique = true, length = 512)
	private String refresh;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
