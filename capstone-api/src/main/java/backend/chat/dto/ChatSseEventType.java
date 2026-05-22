package backend.chat.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChatSseEventType {
	MESSAGE("message"),
	READ("read"),
	TYPING("typing"),
	/** 채팅 목록 화면의 unread 배지 갱신용. 유저 레벨 SSE 채널로 전달된다. */
	BADGE_UPDATE("badge_update");

	private final String value;

	ChatSseEventType(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
