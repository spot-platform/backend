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
	CHECKLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "S007", "Checklist item not found"),
	SPOT_RESOURCE_MISMATCH(HttpStatus.BAD_REQUEST, "S008", "Resource does not belong to this spot"),
	FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "S009", "File not found"),

	// Simulation
	SIMULATION_RUN_NOT_FOUND(HttpStatus.NOT_FOUND, "SIM001", "Simulation run not found"),
	INVALID_TICK_WINDOW(HttpStatus.BAD_REQUEST, "SIM002", "Invalid tick window"),

	// Chat
	CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CH001", "Chat room not found"),
	GROUP_CHAT_REQUIRES_SPOT(HttpStatus.BAD_REQUEST, "CH002", "Group chat room requires a spotId"),

	// User
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User not found"),
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "Email already exists"),
	INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U003", "Password does not match"),
	PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "U004", "New password and confirm password do not match"),
	SOCIAL_USER_CANNOT_CHANGE_PASSWORD(HttpStatus.FORBIDDEN, "U005", "Social login users cannot change password"),
	USER_ALREADY_DELETED(HttpStatus.GONE, "U006", "User account has been deleted");

	private final HttpStatus status;
	private final String code;
	private final String message;

	ErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
}
