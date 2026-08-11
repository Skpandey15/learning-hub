package com.learninghub.shared.web;

import com.learninghub.shared.error.ApiException;
import com.learninghub.shared.error.ErrorCode;
import com.learninghub.shared.error.ErrorFingerprint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public final class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final ProblemDetailsFactory problems;

    public ApiExceptionHandler(ProblemDetailsFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(ApiException.class)
    ProblemDetail apiException(ApiException exception, HttpServletRequest request) {
        ErrorCode code = exception.errorCode();
        LOGGER.atLevel(code.status().is5xxServerError() ? org.slf4j.event.Level.ERROR : org.slf4j.event.Level.WARN)
                .addKeyValue("event.action", "api_error")
                .addKeyValue("error.code", code.name())
                .addKeyValue("http.response.status_code", code.status().value())
                .addKeyValue("error.type", exception.getClass().getName())
                .addKeyValue("error.fingerprint", ErrorFingerprint.of(exception))
                .log("Handled application error");
        return problems.create(code, request, exception.safeProperties());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<Map<String, String>> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
                .toList();
        return problems.create(ErrorCode.VALIDATION_FAILED, request, Map.of("violations", violations));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail constraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        List<Map<String, String>> violations = exception.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
                .toList();
        return problems.create(ErrorCode.VALIDATION_FAILED, request, Map.of("violations", violations));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    ProblemDetail malformedRequest(Exception exception, HttpServletRequest request) {
        return problems.create(ErrorCode.MALFORMED_REQUEST, request, Map.of());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    ProblemDetail noHandler(NoHandlerFoundException exception, HttpServletRequest request) {
        return problems.create(ErrorCode.RESOURCE_NOT_FOUND, request, Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail methodNotAllowed(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED,
                "The HTTP method is not supported for this resource.");
        problem.setTitle("Method not allowed");
        problem.setProperty("code", "METHOD_NOT_ALLOWED");
        problem.setProperty("correlationId", request.getAttribute(CorrelationIdFilter.ATTRIBUTE));
        problem.setProperty("allowedMethods", exception.getSupportedHttpMethods());
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return problems.create(ErrorCode.ACCESS_DENIED, request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
        LOGGER.atError()
                .addKeyValue("event.action", "unhandled_exception")
                .addKeyValue("error.code", ErrorCode.INTERNAL_ERROR.name())
                .addKeyValue("error.type", exception.getClass().getName())
                .addKeyValue("error.fingerprint", ErrorFingerprint.of(exception))
                .log("Unhandled request exception");
        return problems.create(ErrorCode.INTERNAL_ERROR, request, Map.of());
    }
}
