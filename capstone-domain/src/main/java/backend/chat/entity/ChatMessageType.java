package backend.chat.entity;

/**
 * 채팅 메시지의 분류. 응답 DTO 에서 클라이언트가 렌더링 방식을 분기하는 데 사용된다.
 *
 * <ul>
 *   <li>{@link #USER} — 사용자가 직접 보낸 일반 텍스트 메시지.</li>
 *   <li>{@link #IMAGE} — 이미지 첨부 메시지. fileUrl 에 이미지 URL.</li>
 *   <li>{@link #FILE} — 파일 첨부 메시지. fileUrl / fileName / fileSizeBytes 포함.</li>
 *   <li>{@link #SYSTEM} — 서버가 생성한 안내성 메시지 (예: "OO님이 나갔습니다").
 *       senderId 는 고정 값 {@code "SYSTEM"} 으로 저장된다.</li>
 * </ul>
 */
public enum ChatMessageType {
	USER,
	IMAGE,
	FILE,
	SYSTEM
}
