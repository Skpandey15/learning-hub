package com.learninghub.shared.web;

import com.learninghub.shared.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public final class ProblemDetailsFactory {
    public ProblemDetail create(
            ErrorCode code,
            HttpServletRequest request,
            Map<String, Object> safeProperties) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), code.detail());
        problem.setTitle(code.title());
        problem.setType(URI.create(code.type()));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code.name());
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        safeProperties.forEach(problem::setProperty);
        return problem;
    }
}
