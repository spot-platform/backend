package backend.chat.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChatSseEventType {
	MESSAGE("message"),
	READ("read"),
	TYPING("typing"),
	/** 채팅 목록 화면의 unread 배지 갱신용. 유저 레벨 SSE 채널로 전달된다. */
	BADGE_UPDATE("badge_update"),
	/** 투표 생성. 채팅방 멤버 전원에게 브로드캐스트. */
	VOTE_CREATED("vote_created"),
	/** 투표 참여/해제로 결과가 변경됨. */
	VOTE_UPDATED("vote_updated"),
	/** 투표 마감 (수동 또는 자동). */
	VOTE_CLOSED("vote_closed"),
	/** 투표 30분 전 마감 예고 알림. */
	VOTE_CLOSING_SOON("vote_closing_soon");

	private final String value;

	ChatSseEventType(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
