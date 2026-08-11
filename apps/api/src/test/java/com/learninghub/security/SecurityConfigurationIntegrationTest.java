package com.learninghub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/unreachable",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
class SecurityConfigurationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsHealthAndProtectsApplicationRoutesWithProblemJson() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/api/v1/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", "application/problem+json;charset=UTF-8"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("UNAUTHENTICATED", "correlationId");
    }

    @Test
    void requiresCsrfForStateChangingRequestsWithoutBearerAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/protected"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/protected").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
