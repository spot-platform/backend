package backend.spot.entity;

/**
 * 프론트에서 표시하는 스팟 구성원 역할.
 *
 * - OWNER     : 피드/스팟 작성자
 * - SUPPORTER : 알려주는 사람
 * - PARTNER   : 일반 참여자
 */
public enum SpotMemberRole {
	OWNER,
	SUPPORTER,
	PARTNER
}
