package com.learninghub.security;

import com.learninghub.shared.error.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public final class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final SecurityProblemWriter writer;
    private final SecurityAuditService audit;

    public ProblemAuthenticationEntryPoint(SecurityProblemWriter writer, SecurityAuditService audit) {
        this.writer = writer;
        this.audit = audit;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        audit.record(null, request, "AUTHENTICATION", request.getRequestURI(), "DENIED");
        writer.write(ErrorCode.UNAUTHENTICATED, request, response);
    }
}
