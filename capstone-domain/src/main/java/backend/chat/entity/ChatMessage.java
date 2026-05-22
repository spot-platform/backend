package backend.chat.entity;

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
@Table(name = "chat_messages")
public class ChatMessage {

	/**
	 * SYSTEM 메시지가 사용하는 고정 senderId.
	 * senderId 컬럼은 NOT NULL 이므로 실제 유저 ID 대신 본 sentinel 값을 저장한다.
	 * 응답 DTO 는 {@link #type} 으로 분기 — senderId 자체를 검사하지 않아도 된다.
	 */
	public static final String SYSTEM_SENDER_ID = "SYSTEM";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "room_id", nullable = false)
	private Long chatRoomId;

	@Column(nullable = false)
	private String senderId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private ChatMessageType type = ChatMessageType.USER;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	/** IMAGE / FILE 타입 메시지의 파일 URL. */
	@Column(name = "file_url")
	private String fileUrl;

	/** FILE 타입 메시지의 원본 파일명. */
	@Column(name = "file_name")
	private String fileName;

	/** FILE 타입 메시지의 파일 크기 (bytes). */
	@Column(name = "file_size_bytes")
	private Long fileSizeBytes;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
