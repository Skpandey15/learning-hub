package com.learninghub.security;

import com.learninghub.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityAuditService {
    private final SecurityAuditRepository repository;
    private final String salt;
    SecurityAuditService(SecurityAuditRepository repository, @Value("${security.audit.ip-hash-salt}") String salt) {
        this.repository = repository; this.salt = salt;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Authentication auth, HttpServletRequest request, String action, String resource, String outcome) {
        String actor = auth == null ? "anonymous" : auth.getName();
        String roles = auth == null ? "" : String.join(",", authorities(auth));
        String correlation = String.valueOf(request.getAttribute(CorrelationIdFilter.ATTRIBUTE));
        repository.save(new SecurityAuditEvent(actor, roles, action, resource, outcome, correlation,
                hash(request.getRemoteAddr()), null));
    }
    private static Set<String> authorities(Authentication auth) {
        return auth.getAuthorities().stream().map(Object::toString).collect(java.util.stream.Collectors.toSet());
    }
    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest((salt + value).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
