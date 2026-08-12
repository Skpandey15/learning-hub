package com.learninghub.security;

import com.learninghub.shared.error.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public final class ProblemAccessDeniedHandler implements AccessDeniedHandler {
    private final SecurityProblemWriter writer;
    private final SecurityAuditService audit;

    public ProblemAccessDeniedHandler(SecurityProblemWriter writer, SecurityAuditService audit) {
        this.writer = writer;
        this.audit = audit;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException, ServletException {
        audit.record(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication(),
                request, "AUTHORIZATION", request.getRequestURI(), "DENIED");
        writer.write(ErrorCode.ACCESS_DENIED, request, response);
    }
}
