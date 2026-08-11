package com.learninghub.security;

import com.learninghub.shared.error.ErrorCode;
import com.learninghub.shared.web.ProblemDetailsFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public final class SecurityProblemWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityProblemWriter.class);
    private final ObjectMapper objectMapper;
    private final ProblemDetailsFactory problems;

    public SecurityProblemWriter(ObjectMapper objectMapper, ProblemDetailsFactory problems) {
        this.objectMapper = objectMapper;
        this.problems = problems;
    }

    public void write(ErrorCode code, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        LOGGER.atWarn()
                .addKeyValue("event.action", "security_denial")
                .addKeyValue("error.code", code.name())
                .addKeyValue("http.response.status_code", code.status().value())
                .log("Request rejected by security policy");
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        if (code == ErrorCode.UNAUTHENTICATED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        objectMapper.writeValue(response.getOutputStream(), problems.create(code, request, Map.of()));
    }
}
