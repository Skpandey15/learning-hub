package com.learninghub.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

class SecurityConfigurationTest {
    @Test
    void recognizesOnlyNonEmptyBearerAuthorization() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThat(SecurityConfiguration.hasBearerToken(request)).isFalse();

        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic credentials");
        assertThat(SecurityConfiguration.hasBearerToken(request)).isFalse();

        request.removeHeader(HttpHeaders.AUTHORIZATION);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ");
        assertThat(SecurityConfiguration.hasBearerToken(request)).isFalse();

        request.removeHeader(HttpHeaders.AUTHORIZATION);
        request.addHeader(HttpHeaders.AUTHORIZATION, "bearer access-token");
        assertThat(SecurityConfiguration.hasBearerToken(request)).isTrue();
    }
}
