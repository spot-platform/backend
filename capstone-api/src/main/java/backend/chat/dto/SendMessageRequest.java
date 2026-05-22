package backend.chat.dto;

import backend.chat.entity.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "채팅 메시지 전송 요청 DTO")
public class SendMessageRequest {

	@Schema(description = "메시지 타입. 기본값 USER (일반 텍스트). IMAGE / FILE 전송 시 명시.", example = "USER")
	private ChatMessageType type = ChatMessageType.USER;

	@NotBlank
	@Schema(description = "메시지 내용. 파일 타입일 때도 간단한 설명을 넣을 수 있음.", example = "안녕하세요")
	private String content;

	@Schema(description = "IMAGE / FILE 타입 전송 시 파일 URL (S3 presigned URL 등)", example = "https://cdn.example.com/files/abc.jpg")
	private String fileUrl;

	@Schema(description = "FILE 타입의 원본 파일명", example = "report.pdf")
	private String fileName;

	@Schema(description = "FILE 타입의 파일 크기 (bytes)", example = "204800")
	private Long fileSizeBytes;
}
