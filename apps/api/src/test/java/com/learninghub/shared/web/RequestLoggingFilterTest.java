package com.learninghub.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {
    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void logsSuccessfulAndFailedResponsesWithoutChangingThem() throws ServletException, IOException {
        MockHttpServletRequest successRequest = new MockHttpServletRequest("GET", "/api/v1/resource");
        MockHttpServletResponse successResponse = new MockHttpServletResponse();
        filter.doFilter(successRequest, successResponse, (request, response) -> {});

        MockHttpServletRequest failureRequest = new MockHttpServletRequest("POST", "/api/v1/resource");
        MockHttpServletResponse failureResponse = new MockHttpServletResponse();
        filter.doFilter(failureRequest, failureResponse, (request, response) -> failureResponse.setStatus(503));

        assertThat(successResponse.getStatus()).isEqualTo(200);
        assertThat(failureResponse.getStatus()).isEqualTo(503);
    }

    @Test
    void skipsHighVolumeLivenessProbeOnly() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/actuator/health/liveness")))
                .isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/actuator/health/readiness")))
                .isFalse();
    }
}
