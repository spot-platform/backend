package backend.spot.entity;

/**
 * 스팟 참여자의 참여 상태를 나타내는 열거형
 *
 * - ACTIVE  : 현재 스팟에 참여 중
 * - LEFT    : 스팟에서 나간 상태
 * - EXPELLED: 강제 퇴출된 상태
 */
public enum ParticipantState {
	ACTIVE,
	LEFT,
	EXPELLED
}
