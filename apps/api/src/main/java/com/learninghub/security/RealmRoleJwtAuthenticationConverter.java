package com.learninghub.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public final class RealmRoleJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {
    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new LinkedHashSet<>();
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> access && access.get("roles") instanceof Collection<?> roles) {
            roles.stream().map(String::valueOf).map(String::toUpperCase)
                    .filter(role -> isLearningRole(role))
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .forEach(authorities::add);
        }
        String principal = jwt.getClaimAsString("preferred_username");
        return new JwtAuthenticationToken(jwt, authorities, principal == null ? jwt.getSubject() : principal);
    }

    private static boolean isLearningRole(String role) {
        for (LearningRole learningRole : LearningRole.values()) {
            if (learningRole.name().equals(role)) return true;
        }
        return false;
    }
}
