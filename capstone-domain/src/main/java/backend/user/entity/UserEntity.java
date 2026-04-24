package backend.user.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
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
@Table(name = "users")
public class UserEntity {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@Column(columnDefinition = "VARCHAR(36)")
	private String id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String password;

	@Builder.Default
	@Column(nullable = false)
	private Boolean isLock = false;

	@Builder.Default
	@Column(nullable = false)
	private Boolean isSocial = false;

	@Enumerated(EnumType.STRING)
	@Column
	private SocialProviderType socialProviderType;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(nullable = false)
	private UserRoleType roleType = UserRoleType.USER;

	@Builder.Default
	@Column(nullable = false)
	private Boolean isVerified = false;

	@Enumerated(EnumType.STRING)
	@Column
	private ProfileType profileType;

	@Column(nullable = false)
	private String nickname;

	@Column
	private String phone;

	@Column
	private String avatarUrl;

	@Builder.Default
	@Column(nullable = false)
	private Long pointBalance = 0L;

	@Builder.Default
	@Column(nullable = false)
	private Boolean isDeleted = false;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	public void updateProfile(String nickname, String email, String phone, String avatarUrl) {
		if (nickname != null) {
			this.nickname = nickname;
		}
		if (email != null) {
			this.email = email;
		}
		if (phone != null) {
			this.phone = phone;
		}
		if (avatarUrl != null) {
			this.avatarUrl = avatarUrl;
		}
	}

	public void changePassword(String encodedNewPassword) {
		this.password = encodedNewPassword;
	}

	public void softDelete() {
		this.isDeleted = true;
	}

	public void chargePoint(long amount) {
		this.pointBalance += amount;
	}

	public void usePoint(long amount) {
		this.pointBalance -= amount;
	}
}
