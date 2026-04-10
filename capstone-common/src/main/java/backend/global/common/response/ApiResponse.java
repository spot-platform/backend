package backend.global.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {
	private int status;
	private String message;
	private T data;

	private ApiResponse(int status, String message, T data) {
		this.status = status;
		this.message = message;
		this.data = data;
	}

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(200, "Success", data);
	}

	public static ApiResponse<Void> success() {
		return new ApiResponse<>(200, "Success", null);
	}

	public static <T> ApiResponse<T> error(int status, String message) {
		return new ApiResponse<>(status, message, null);
	}
}
