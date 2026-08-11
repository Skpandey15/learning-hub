package com.learninghub.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.learninghub.shared.error.ErrorCode;
import com.learninghub.shared.web.CorrelationIdFilter;
import com.learninghub.shared.web.ProblemDetailsFactory;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class SecurityProblemWriterTest {
    @Test
    void unauthenticatedResponseUsesProblemJsonAndBearerChallenge() throws IOException {
        SecurityProblemWriter writer = new SecurityProblemWriter(
                new ObjectMapper(), new ProblemDetailsFactory());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/learning/domains");
        request.setAttribute(CorrelationIdFilter.ATTRIBUTE, "request-12345678");
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(ErrorCode.UNAUTHENTICATED, request, response);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getContentAsString()).contains("UNAUTHENTICATED", "request-12345678");
    }
}
