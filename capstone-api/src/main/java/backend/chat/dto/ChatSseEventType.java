package backend.chat.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChatSseEventType {
	MESSAGE("message"),
	READ("read"),
	TYPING("typing");

	private final String value;

	ChatSseEventType(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
