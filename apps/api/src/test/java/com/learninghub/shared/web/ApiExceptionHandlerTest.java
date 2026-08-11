package com.learninghub.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.learninghub.shared.error.ApiException;
import com.learninghub.shared.error.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiExceptionHandlerTest {
    private final ProblemDetailsFactory problems = new ProblemDetailsFactory();
    private final ApiExceptionHandler handler = new ApiExceptionHandler(problems);

    @Test
    void unexpectedExceptionReturnsSafeProblemWithoutLeakingMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/example");
        request.setAttribute(CorrelationIdFilter.ATTRIBUTE, "request-12345678");

        ProblemDetail result = handler.unexpected(
                new IllegalStateException("database-password-must-not-leak"), request);

        assertThat(result.getStatus()).isEqualTo(500);
        assertThat(result.getDetail()).isEqualTo("The request could not be completed.");
        assertThat(result.getDetail()).doesNotContain("database-password");
        assertThat(result.getProperties())
                .containsEntry("code", "INTERNAL_ERROR")
                .containsEntry("correlationId", "request-12345678");
    }

    @Test
    void mapsExpectedApplicationErrorsAndSafeProperties() {
        MockHttpServletRequest request = request();

        ProblemDetail conflict = handler.apiException(
                new ApiException(ErrorCode.CONFLICT, Map.of("currentVersion", 3)), request);
        ProblemDetail unavailable = handler.apiException(
                new ApiException(ErrorCode.DEPENDENCY_UNAVAILABLE, Map.of(), new IOException("hidden")),
                request);

        assertThat(conflict.getStatus()).isEqualTo(409);
        assertThat(conflict.getProperties()).containsEntry("currentVersion", 3);
        assertThat(unavailable.getStatus()).isEqualTo(503);
    }

    @Test
    void mapsBeanAndConstraintValidationViolations() {
        MockHttpServletRequest request = request();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "name", "must not be blank"));
        binding.addError(new FieldError("request", "description", null));
        MethodArgumentNotValidException beanException = new MethodArgumentNotValidException(
                mock(MethodParameter.class), binding);

        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("topic.slug");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be valid");
        ConstraintViolationException constraintException = new ConstraintViolationException(Set.of(violation));

        ProblemDetail beanProblem = handler.validation(beanException, request);
        ProblemDetail constraintProblem = handler.constraintViolation(constraintException, request);

        assertThat(beanProblem.getProperties()).containsKey("violations");
        assertThat(constraintProblem.getProperties()).containsKey("violations");
    }

    @Test
    void mapsMalformedMissingMethodAndDeniedRequests() {
        MockHttpServletRequest request = request();

        assertThat(handler.malformedRequest(new IOException(), request).getStatus()).isEqualTo(400);
        assertThat(handler.noHandler(
                        new NoHandlerFoundException("GET", "/missing", HttpHeaders.EMPTY), request)
                .getStatus()).isEqualTo(404);
        ProblemDetail methodProblem = handler.methodNotAllowed(
                new HttpRequestMethodNotSupportedException("POST", List.of("GET")), request);
        assertThat(methodProblem.getStatus()).isEqualTo(405);
        assertThat(methodProblem.getProperties()).containsKey("allowedMethods");
        assertThat(handler.accessDenied(new AccessDeniedException("hidden"), request).getStatus())
                .isEqualTo(403);
    }

    @Test
    void problemFactoryOmitsAbsentCorrelationId() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/missing");

        ProblemDetail result = problems.create(ErrorCode.RESOURCE_NOT_FOUND, request, Map.of());

        assertThat(result.getProperties()).doesNotContainKey("correlationId");
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/example");
        request.setAttribute(CorrelationIdFilter.ATTRIBUTE, "request-12345678");
        return request;
    }
}
