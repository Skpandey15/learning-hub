package com.learninghub.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.learninghub.shared.web.CorrelationIdFilter;
import com.learninghub.shared.web.ProblemDetailsFactory;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;

class SecurityHandlersTest {
    private final SecurityProblemWriter writer = new SecurityProblemWriter(
            new ObjectMapper(), new ProblemDetailsFactory());

    @Test
    void entryPointAndDeniedHandlerDelegateToProblemWriter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/resource");
        request.setAttribute(CorrelationIdFilter.ATTRIBUTE, "request-12345678");

        MockHttpServletResponse unauthenticated = new MockHttpServletResponse();
        new ProblemAuthenticationEntryPoint(writer).commence(
                request, unauthenticated, new BadCredentialsException("hidden"));

        MockHttpServletResponse denied = new MockHttpServletResponse();
        new ProblemAccessDeniedHandler(writer).handle(
                request, denied, new AccessDeniedException("hidden"));

        assertThat(unauthenticated.getStatus()).isEqualTo(401);
        assertThat(denied.getStatus()).isEqualTo(403);
        assertThat(denied.getHeader("WWW-Authenticate")).isNull();
    }
}
