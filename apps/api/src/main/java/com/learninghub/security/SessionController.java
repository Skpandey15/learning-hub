package com.learninghub.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SessionController {
    private final AccessPolicyService policies;
    private final SecurityAuditService audit;
    SessionController(AccessPolicyService policies, SecurityAuditService audit) { this.policies = policies; this.audit = audit; }

    @GetMapping("/session")
    SessionView session(JwtAuthenticationToken authentication, HttpServletRequest request) {
        audit.record(authentication, request, "SESSION_READ", "/api/v1/session", "ALLOWED");
        return new SessionView(authentication.getName(), authentication.getToken().getClaimAsString("email"),
                roles(authentication), authentication.getToken().getExpiresAt());
    }

    @GetMapping("/study/access")
    AccessView studyAccess(Authentication authentication, HttpServletRequest request) {
        Set<String> roles = roles(authentication);
        policies.requireStudyAccess(roles);
        audit.record(authentication, request, "STUDY_ACCESS", "/api/v1/study", "ALLOWED");
        return AccessView.from(policies.current(), true);
    }

    @GetMapping("/admin/access-policy")
    AccessView policy() { return AccessView.from(policies.current(), true); }

    @PutMapping("/admin/access-policy")
    AccessView update(@Valid @RequestBody UpdateAccessPolicy request, Authentication authentication,
            HttpServletRequest servletRequest) {
        AccessPolicy policy = policies.update(request.sharedStudyAccess(), request.candidateEnabled(),
                request.interviewerEnabled(), authentication.getName());
        audit.record(authentication, servletRequest, "ACCESS_POLICY_UPDATE", "/api/v1/admin/access-policy", "ALLOWED");
        return AccessView.from(policy, true);
    }

    private static Set<String> roles(Authentication auth) {
        return auth.getAuthorities().stream().map(Object::toString).collect(Collectors.toUnmodifiableSet());
    }
    record SessionView(String username, String email, Set<String> roles, Instant expiresAt) {}
    record UpdateAccessPolicy(boolean sharedStudyAccess, boolean candidateEnabled, boolean interviewerEnabled) {}
    record AccessView(boolean allowed, boolean sharedStudyAccess, boolean candidateEnabled,
            boolean interviewerEnabled, Instant updatedAt, String updatedBy) {
        static AccessView from(AccessPolicy p, boolean allowed) {
            return new AccessView(allowed, p.isSharedStudyAccess(), p.isCandidateEnabled(),
                    p.isInterviewerEnabled(), p.getUpdatedAt(), p.getUpdatedBy());
        }
    }
}
