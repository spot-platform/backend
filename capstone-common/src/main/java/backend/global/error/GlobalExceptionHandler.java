package backend.global.error;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import backend.global.common.response.ApiResponse;
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
		log.warn("handleBusinessException: {}", exception.getMessage());
		ErrorCode errorCode = exception.getErrorCode();
		ApiResponse<Void> response = ApiResponse.error(errorCode.getStatus().value(), errorCode.getMessage());
		return new ResponseEntity<>(response, errorCode.getStatus());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	protected ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
		log.warn("handleIllegalArgumentException: {}", exception.getMessage());
		ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(IllegalStateException.class)
	protected ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException exception) {
		log.warn("handleIllegalStateException: {}", exception.getMessage());
		ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.collect(Collectors.joining(", "));
		log.warn("handleValidationException: {}", message);
		ApiResponse<Void> response = ApiResponse.error(
			HttpStatus.BAD_REQUEST.value(),
			message
		);
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
		log.error("handleException", exception);
		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		ApiResponse<Void> response = ApiResponse.error(errorCode.getStatus().value(), errorCode.getMessage());
		return new ResponseEntity<>(response, errorCode.getStatus());
	}
}
