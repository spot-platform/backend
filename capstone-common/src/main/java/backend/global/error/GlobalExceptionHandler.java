package backend.global.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import backend.global.common.response.ApiResponse;
import backend.global.error.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
		log.error("handleException", exception);
		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		ApiResponse<Void> response = ApiResponse.error(errorCode.getStatus().value(), errorCode.getMessage());
		return new ResponseEntity<>(response, errorCode.getStatus());
	}
}
