package backend.spot.entity;

import backend.feed.entity.FeedApplicationRole;

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
	PARTNER;

	public static SpotMemberRole fromApplicationRole(FeedApplicationRole applicationRole) {
		if (applicationRole == null) {
			return null;
		}
		return switch (applicationRole) {
			case SUPPORTER -> SUPPORTER;
			case PARTNER -> PARTNER;
		};
	}

	public static SpotMemberRole fromParticipant(SpotParticipant participant) {
		if (participant.getRole() == ParticipantRole.AUTHOR) {
			return OWNER;
		}
		return fromApplicationRole(participant.getApplicationRole());
	}
}
