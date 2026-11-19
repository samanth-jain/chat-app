package com.example.chatapp.chat.exception;

import com.example.chatapp.chat.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ChatRoomAlreadyExistsException.class)
	public ResponseEntity<ApiError> handleDuplicateRoom(
			ChatRoomAlreadyExistsException exception, HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, exception.getMessage(), request);
	}

	@ExceptionHandler(ChatRoomNotFoundException.class)
	public ResponseEntity<ApiError> handleMissingRoom(
			ChatRoomNotFoundException exception, HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler(ParticipantNotInRoomException.class)
	public ResponseEntity<ApiError> handleParticipantNotInRoom(
			ParticipantNotInRoomException exception, HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, exception.getMessage(), request);
	}

	@ExceptionHandler(InvalidHistoryLimitException.class)
	public ResponseEntity<ApiError> handleInvalidLimit(
			InvalidHistoryLimitException exception, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleInvalidBody(
			MethodArgumentNotValidException exception, HttpServletRequest request) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + " " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return error(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadableBody(
			HttpMessageNotReadableException exception, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "Request body is missing or malformed.", request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> handleInvalidParameter(
			MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "Request parameter '" + exception.getName() + "' is invalid.", request);
	}

	private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request) {
		return ResponseEntity.status(status).body(
				new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
	}
}
