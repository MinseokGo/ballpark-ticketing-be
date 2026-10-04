package com.ballpark.ticketing.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 모든 에러 응답을 RFC 9457 ProblemDetail 형식으로 통일한다.
 * 표준 필드(type, title, status, detail, instance)에 더해 {@code code}(ErrorCode)를,
 * 입력값 검증 실패 시에는 {@code errors}(필드별 사유)를 함께 내려준다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String CODE = "code";
	private static final String ERRORS = "errors";

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException e, HttpServletRequest request) {
		ErrorCode errorCode = e.getErrorCode();
		log.info("Business exception: code={}, detail={}", errorCode.getCode(), e.getMessage());
		return ResponseEntity.status(errorCode.getStatus())
				.body(problemDetail(errorCode, e.getMessage(), request));
	}

	/**
	 * {@code @Validated}가 붙은 빈의 메서드 검증(AOP)에서 발생한다.
	 * Spring MVC 내장 검증의 HandlerMethodValidationException과 같은 형식으로 응답한다.
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> handleConstraintViolation(
			ConstraintViolationException e, HttpServletRequest request) {
		ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
		ProblemDetail problemDetail = problemDetail(errorCode, errorCode.getMessage(), request);
		problemDetail.setProperty(ERRORS, e.getConstraintViolations().stream()
				.map(violation -> fieldError(lastNode(violation.getPropertyPath()), violation.getMessage()))
				.toList());
		return ResponseEntity.status(errorCode.getStatus()).body(problemDetail);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception e, HttpServletRequest request) {
		log.error("Unexpected exception", e);
		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		return ResponseEntity.status(errorCode.getStatus())
				.body(problemDetail(errorCode, errorCode.getMessage(), request));
	}

	/**
	 * Spring MVC 표준 예외(400 바인딩 실패, 404, 405, 415 등)는 부모 클래스가 ProblemDetail을 만든다.
	 * 여기서는 그 응답에 code, instance와 검증 실패 상세만 덧붙인다.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
		if (body == null && ex instanceof ErrorResponse errorResponse) {
			body = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
		}
		if (body instanceof ProblemDetail problemDetail) {
			ErrorCode errorCode = toErrorCode(statusCode);
			problemDetail.setProperty(CODE, errorCode.getCode());
			if (problemDetail.getInstance() == null && request instanceof ServletWebRequest servletWebRequest) {
				problemDetail.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
			}
			List<Map<String, String>> fieldErrors = fieldErrors(ex);
			if (!fieldErrors.isEmpty()) {
				problemDetail.setDetail(ErrorCode.INVALID_INPUT_VALUE.getMessage());
				problemDetail.setProperty(ERRORS, fieldErrors);
			}
		}
		return super.handleExceptionInternal(ex, body, headers, statusCode, request);
	}

	private static List<Map<String, String>> fieldErrors(Exception ex) {
		if (ex instanceof MethodArgumentNotValidException e) {
			return e.getBindingResult().getFieldErrors().stream()
					.map(error -> fieldError(error.getField(), error.getDefaultMessage()))
					.toList();
		}
		if (ex instanceof HandlerMethodValidationException e) {
			return e.getParameterValidationResults().stream()
					.flatMap(result -> result.getResolvableErrors().stream()
							.map(error -> fieldError(
									error instanceof FieldError fieldError
											? fieldError.getField()
											: result.getMethodParameter().getParameterName(),
									error.getDefaultMessage())))
					.toList();
		}
		return List.of();
	}

	private static Map<String, String> fieldError(@Nullable String field, @Nullable String reason) {
		return Map.of(
				"field", field == null ? "" : field,
				"reason", reason == null ? "" : reason);
	}

	private static @Nullable String lastNode(Path path) {
		String name = null;
		for (Path.Node node : path) {
			name = node.getName();
		}
		return name;
	}

	private static ErrorCode toErrorCode(HttpStatusCode statusCode) {
		return switch (statusCode.value()) {
			case 400 -> ErrorCode.INVALID_INPUT_VALUE;
			case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
			case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
			case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
			default -> statusCode.is5xxServerError() ? ErrorCode.INTERNAL_SERVER_ERROR : ErrorCode.INVALID_REQUEST;
		};
	}

	private static ProblemDetail problemDetail(ErrorCode errorCode, String detail, HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), detail);
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		problemDetail.setProperty(CODE, errorCode.getCode());
		return problemDetail;
	}
}
