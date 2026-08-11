package com.learninghub.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "The request failed validation.");
        problem.setTitle("Invalid request");
        problem.setType(URI.create("https://learninghub.dev/problems/validation"));
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("correlationId", request.getAttribute(CorrelationIdFilter.HEADER));
        return problem;
    }
}
