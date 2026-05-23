package backend.global.error.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
	// Common
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid Input Value"),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "Method Not Allowed"),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "Internal Server Error"),
	ENTITY_NOT_FOUND(HttpStatus.BAD_REQUEST, "C004", "Entity Not Found"),

	// Auth
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "Invalid token"),
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "Token has expired"),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "Invalid refresh token"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A004", "Authentication required"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "A005", "Access denied"),

	// Spot
	SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "Spot not found"),
	INVALID_SPOT_STATUS(HttpStatus.BAD_REQUEST, "S002", "Invalid spot status transition"),
	VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "S003", "Vote not found"),
	OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "S004", "Vote option not found"),
	OPTION_NOT_IN_VOTE(HttpStatus.BAD_REQUEST, "S005", "Option does not belong to this vote"),
	ALREADY_VOTED(HttpStatus.CONFLICT, "S006", "Already voted on this vote"),
	VOTE_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "S010", "Vote is not active"),
	SINGLE_SELECT_VOTE_LIMIT(HttpStatus.BAD_REQUEST, "S011", "Single-select vote allows at most one option"),
	CHECKLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "S007", "Checklist item not found"),
	CHECKLIST_ASSIGNEE_NOT_PARTICIPANT(HttpStatus.BAD_REQUEST, "S012", "Assignee must be a spot participant"),
	NOT_SPOT_PARTICIPANT(HttpStatus.FORBIDDEN, "S013", "Only spot participants can perform this action"),
	SPOT_RESOURCE_MISMATCH(HttpStatus.BAD_REQUEST, "S008", "Resource does not belong to this spot"),
	FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "S009", "File not found"),

	// Simulation
	SIMULATION_RUN_NOT_FOUND(HttpStatus.NOT_FOUND, "SIM001", "Simulation run not found"),
	INVALID_TICK_WINDOW(HttpStatus.BAD_REQUEST, "SIM002", "Invalid tick window"),

	// Chat
	CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CH001", "Chat room not found"),
	GROUP_CHAT_REQUIRES_SPOT(HttpStatus.BAD_REQUEST, "CH002", "Group chat room requires a spotId"),
	CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CH003", "You are not a member of this chat room"),
	CHAT_PARTNER_NOT_FOUND(HttpStatus.NOT_FOUND, "CH004", "Chat partner user not found"),
	CHAT_PERSONAL_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CH005", "Cannot start a personal chat with yourself"),
	CHAT_PERSONAL_REQUIRES_PARTNER(HttpStatus.BAD_REQUEST, "CH006", "Personal chat requires a partnerId"),
	CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CH007", "Chat message not found"),
	CHAT_MESSAGE_NOT_IN_ROOM(HttpStatus.BAD_REQUEST, "CH008", "Message does not belong to this chat room"),
	CHAT_BLOCKED_BETWEEN_USERS(HttpStatus.FORBIDDEN, "CH009", "Personal chat is blocked between the two users"),
	CHAT_BLOCK_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "CH010", "Block target user not found"),
	CHAT_SELF_BLOCK_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CH011", "Cannot block yourself"),
	CHAT_ROOM_READ_ONLY(HttpStatus.FORBIDDEN, "CH012", "This chat room is read-only (spot completed or cancelled)"),
	CHAT_VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "CH013", "Chat vote not found"),
	CHAT_VOTE_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "CH014", "Chat vote is not active"),
	CHAT_VOTE_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "CH015", "Chat vote option not found"),
	CHAT_VOTE_OPTION_NOT_IN_VOTE(HttpStatus.BAD_REQUEST, "CH016", "Option does not belong to this vote"),
	CHAT_VOTE_SINGLE_SELECT_LIMIT(HttpStatus.BAD_REQUEST, "CH017", "Single-select vote allows at most one option"),
	CHAT_VOTE_NOT_CREATOR(HttpStatus.FORBIDDEN, "CH018", "Only the vote creator can perform this action"),

	// Post
	POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "Post not found"),

	// Notification
	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "Notification not found"),

	// User
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User not found"),
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "Email already exists"),
	INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U003", "Password does not match"),
	PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "U004", "New password and confirm password do not match"),
	SOCIAL_USER_CANNOT_CHANGE_PASSWORD(HttpStatus.FORBIDDEN, "U005", "Social login users cannot change password"),
	USER_ALREADY_DELETED(HttpStatus.GONE, "U006", "User account has been deleted"),

	// Pay
	INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "PAY001", "Charge amount must be at least 1000"),
	// 출금/사용 API용 (현재 미사용, 후속 작업에서 사용 예정)
	INSUFFICIENT_POINT_BALANCE(HttpStatus.BAD_REQUEST, "PAY002", "Insufficient point balance");

	private final HttpStatus status;
	private final String code;
	private final String message;

	ErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
}
