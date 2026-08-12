package com.learninghub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthenticationAuthorizationTest {
    @Test
    void convertsOnlySupportedRealmRolesAndUsesPreferredUsername() {
        Jwt jwt = jwt(Map.of("roles", List.of("candidate", "offline_access", "ADMIN")), "alice");
        JwtAuthenticationToken token = new RealmRoleJwtAuthenticationConverter().convert(jwt);
        assertThat(token.getName()).isEqualTo("alice");
        assertThat(token.getAuthorities()).extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_CANDIDATE", "ROLE_ADMIN");
    }

    @Test
    void fallsBackToSubjectWhenRealmClaimsAreMissing() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("subject-1")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        assertThat(new RealmRoleJwtAuthenticationConverter().convert(jwt).getName()).isEqualTo("subject-1");
        Jwt malformed = Jwt.withTokenValue("token").header("alg", "none").subject("subject-2")
                .claim("realm_access", Map.of("roles", "candidate"))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        assertThat(new RealmRoleJwtAuthenticationConverter().convert(malformed).getAuthorities()).isEmpty();
    }

    @Test
    void enforcesConfiguredRoleAccessAndAllowsAdmins() {
        AccessPolicyRepository repository = mock(AccessPolicyRepository.class);
        when(repository.findById((short) 1)).thenReturn(Optional.of(new AccessPolicy(true, true, false, "system")));
        AccessPolicyService service = new AccessPolicyService(repository);
        service.requireStudyAccess(Set.of("ROLE_CANDIDATE"));
        service.requireStudyAccess(Set.of("ROLE_ADMIN"));
        assertThatThrownBy(() -> service.requireStudyAccess(Set.of("ROLE_INTERVIEWER")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.requireStudyAccess(Set.of()))
                .isInstanceOf(AccessDeniedException.class);
        when(repository.findById((short) 1)).thenReturn(Optional.of(new AccessPolicy(false, false, true, "system")));
        service.requireStudyAccess(Set.of("ROLE_INTERVIEWER"));
        assertThatThrownBy(() -> service.requireStudyAccess(Set.of("ROLE_CANDIDATE")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void readsAndUpdatesSingletonPolicy() {
        AccessPolicyRepository repository = mock(AccessPolicyRepository.class);
        AccessPolicy policy = new AccessPolicy(true, true, true, "system");
        when(repository.findById((short) 1)).thenReturn(Optional.of(policy));
        AccessPolicyService service = new AccessPolicyService(repository);
        assertThat(service.current()).isSameAs(policy);
        AccessPolicy updated = service.update(false, true, false, "admin");
        assertThat(updated.isSharedStudyAccess()).isFalse();
        assertThat(updated.isCandidateEnabled()).isTrue();
        assertThat(updated.isInterviewerEnabled()).isFalse();
        assertThat(updated.getUpdatedBy()).isEqualTo("admin");
        assertThat(updated.getUpdatedAt()).isNotNull();
        when(repository.findById((short) 1)).thenReturn(Optional.empty());
        assertThatThrownBy(service::current).isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void recordsPrivacyPreservingAuditEvent() {
        SecurityAuditRepository repository = mock(SecurityAuditRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityAuditService service = new SecurityAuditService(repository, "salt");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.0.2.4");
        when(request.getAttribute(any())).thenReturn("correlation-123");
        service.record(new TestingAuthenticationToken("alice", null, "ROLE_ADMIN"), request,
                "UPDATE", "/resource", "ALLOWED");
        service.record(null, request, "READ", "/resource", "DENIED");
        verify(repository, org.mockito.Mockito.times(2)).save(any(SecurityAuditEvent.class));
    }

    @Test
    void sessionControllerReturnsSessionAccessAndAdminPolicy() {
        AccessPolicyService policies = mock(AccessPolicyService.class);
        SecurityAuditService audit = mock(SecurityAuditService.class);
        AccessPolicy policy = new AccessPolicy(true, true, true, "system");
        when(policies.current()).thenReturn(policy);
        when(policies.update(false, true, false, "alice")).thenReturn(policy);
        SessionController controller = new SessionController(policies, audit);
        HttpServletRequest request = mock(HttpServletRequest.class);
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt(Map.of("roles", List.of("candidate")), "alice"),
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CANDIDATE")), "alice");
        assertThat(controller.session(auth, request).username()).isEqualTo("alice");
        assertThat(controller.studyAccess(auth, request).allowed()).isTrue();
        assertThat(controller.policy().sharedStudyAccess()).isTrue();
        assertThat(controller.update(new SessionController.UpdateAccessPolicy(false, true, false), auth, request))
                .isNotNull();
        verify(policies).update(false, true, false, "alice");
    }

    private static Jwt jwt(Map<String, Object> realmAccess, String username) {
        return Jwt.withTokenValue("token").header("alg", "none").subject("subject-1")
                .claim("preferred_username", username).claim("email", "alice@example.com")
                .claim("realm_access", realmAccess).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }
}
