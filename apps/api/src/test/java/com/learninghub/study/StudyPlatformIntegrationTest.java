package com.learninghub.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learninghub.shared.error.ApiException;
import com.learninghub.study.StudyModels.AiContent;
import com.learninghub.study.StudyModels.AiRequest;
import com.learninghub.study.StudyModels.AiTopic;
import com.learninghub.study.StudyModels.AiUnit;
import com.learninghub.study.StudyModels.SkillLevel;
import com.learninghub.study.StudyModels.UnitType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "generation.worker.enabled=false")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StudyPlatformIntegrationTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.4-alpine");
    static UUID domainId;
    static UUID technologyId;
    static UUID topicId;
    static UUID jobId;
    static UUID versionId;
    static UUID unitId;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired StudyPlatformService service;
    @Autowired ObjectMapper json;

    @Test @Order(1)
    void createsCatalogAndHidesDraftsFromLearners() throws Exception {
        domainId = id(mvc.perform(post("/api/v1/admin/ecosystems").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\"java\",\"name\":\"Java\",\"description\":\"Java ecosystem\",\"displayOrder\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT")).andReturn());
        technologyId = id(mvc.perform(post("/api/v1/admin/ecosystems/{id}/technologies", domainId).with(admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"slug\":\"core-java\",\"name\":\"Core Java\",\"description\":\"Language\",\"displayOrder\":1}"))
                .andExpect(status().isCreated()).andReturn());
        topicId = id(mvc.perform(post("/api/v1/admin/technologies/{id}/topics", technologyId).with(admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"slug\":\"collections\",\"title\":\"Collections\",\"summary\":\"Use collections\",\"skillLevel\":\"BEGINNER\",\"estimatedMinutes\":60,\"objectives\":[\"Compare types\"],\"displayOrder\":1}"))
                .andExpect(status().isCreated()).andReturn());
        mvc.perform(get("/api/v1/learning/domains").with(candidate())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/admin/ecosystems").with(admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"));
    }

    @Test @Order(2)
    void validatesAndPublishesCatalogWithOptimisticLocking() throws Exception {
        mvc.perform(post("/api/v1/admin/ecosystems/{id}/publish", domainId).with(admin()).header("If-Match", "99")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"publish\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/admin/ecosystems/{id}/publish", domainId).with(admin()).header("If-Match", "0")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"initial curriculum\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/learning/domains").with(candidate())).andExpect(jsonPath("$[0].slug").value("java"));
        mvc.perform(get("/api/v1/learning/domains/{id}/technologies", domainId).with(candidate())).andExpect(jsonPath("$[0].slug").value("core-java"));
        mvc.perform(get("/api/v1/learning/technologies/{id}/topics", technologyId).with(candidate())).andExpect(jsonPath("$[0].slug").value("collections"));
        assertThat(service.technologies(domainId, true)).hasSize(1);
        assertThat(service.topics(technologyId, true)).hasSize(1);
        assertThatThrownBy(() -> service.currentContent(topicId, "candidate")).isInstanceOf(ApiException.class);
        assertThat(service.progress("candidate-with-no-content").percent()).isZero();

        StudyModels.CreateDomain emptyDomain = new StudyModels.CreateDomain("empty", "Empty", "No curriculum", 2);
        UUID emptyId = service.createDomain(emptyDomain, "admin", "test").id();
        assertThatThrownBy(() -> service.publishDomain(emptyId, 0, "admin", "test", "invalid"))
                .isInstanceOf(ApiException.class);
        UUID draftTechnology = service.createTechnology(emptyId,
                new StudyModels.CreateTechnology("draft", "Draft", "Draft technology", 1), "admin", "test").id();
        UUID draftTopic = service.createTopic(draftTechnology,
                new StudyModels.CreateTopic("draft-topic", "Draft topic", "Not published", StudyModels.SkillLevel.BEGINNER,
                        30, List.of("Learn"), 1), "admin", "test").id();
        assertThatThrownBy(() -> service.requestGeneration(draftTopic, "admin", "draft-key", "test"))
                .isInstanceOf(ApiException.class);
    }

    @Test @Order(3)
    void createsIdempotentJobClaimsAndPersistsValidatedDraft() throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/admin/topics/{id}/generation-jobs", topicId).with(admin())
                .header("Idempotency-Key", "generation-1")).andExpect(status().isAccepted()).andReturn();
        jobId = id(created);
        mvc.perform(post("/api/v1/admin/topics/{id}/generation-jobs", topicId).with(admin())
                .header("Idempotency-Key", "generation-1")).andExpect(jsonPath("$.id").value(jobId.toString()));
        assertThat(service.claimNextJob()).isEqualTo(jobId);
        assertThat(service.claimNextJob()).isNull();
        service.persistDraft(jobId, topicId, validContent(topicId, "Collections"));
        MvcResult result = mvc.perform(get("/api/v1/admin/generation-jobs/{id}", jobId).with(admin()))
                .andExpect(jsonPath("$.status").value("AWAITING_REVIEW")).andReturn();
        versionId = UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("resultVersionId").asText());
        MvcResult draft = mvc.perform(get("/api/v1/admin/content-versions/{id}", versionId).with(admin()))
                .andExpect(jsonPath("$.units.length()").value(4)).andReturn();
        unitId = UUID.fromString(json.readTree(draft.getResponse().getContentAsString()).get("units").get(0).get("id").asText());
        assertThatThrownBy(() -> service.publishContent(versionId, 99, "admin", "test", "wrong version"))
                .isInstanceOf(ApiException.class);
    }

    @Test @Order(4)
    void publishesImmutableContentAndTracksOwnProgress() throws Exception {
        mvc.perform(post("/api/v1/admin/content-versions/{id}/publish", versionId).with(admin()).header("If-Match", "0")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"reviewed for accuracy\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versionNumber").value(1));
        mvc.perform(get("/api/v1/learning/topics/{id}/content", topicId).with(candidate()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Collections"));
        mvc.perform(put("/api/v1/learning/units/{id}/completion", unitId).with(candidate())
                .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true}"))
                .andExpect(jsonPath("$.completed").value(true));
        mvc.perform(get("/api/v1/learning/progress/me").with(candidate()))
                .andExpect(jsonPath("$.completedUnits").value(1)).andExpect(jsonPath("$.percent").value(25.0));
        mvc.perform(put("/api/v1/learning/units/{id}/completion", unitId).with(candidate())
                .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":false}"))
                .andExpect(jsonPath("$.completedAt").doesNotExist());
        assertThatThrownBy(() -> service.draftContent(UUID.randomUUID(), "admin")).isInstanceOf(ApiException.class);
    }

    @Test @Order(5)
    void rejectsMalformedUnsafeAndUnknownOperations() throws Exception {
        mvc.perform(post("/api/v1/admin/ecosystems").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\"Bad Slug\",\"name\":\"\",\"description\":\"x\",\"displayOrder\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/ecosystems/{id}/publish", domainId).with(admin()).header("If-Match", "bad")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"retry\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/generation-jobs/{id}", UUID.randomUUID()).with(admin())).andExpect(status().isNotFound());
        assertThatThrownBy(() -> service.complete(UUID.randomUUID(), true, "candidate"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.persistDraft(jobId, topicId, validContent(UUID.randomUUID(), "Wrong")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.persistDraft(jobId, topicId,
                new AiContent(topicId, "Missing", "Long introduction", null, "Long conclusion", "model", "prompt")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.persistDraft(jobId, topicId, contentWithUnits(topicId, 3, false, false)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.persistDraft(jobId, topicId, contentWithUnits(topicId, 13, false, false)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.persistDraft(jobId, topicId, contentWithUnits(topicId, 4, true, false)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.persistDraft(jobId, topicId, contentWithUnits(topicId, 4, false, true)))
                .isInstanceOf(ApiException.class);
        AiContent unsafe = new AiContent(topicId, "Unsafe", "Long enough introduction", List.of(
                new AiUnit("same", UnitType.THEORY, "A", "<script>unsafe</script> content", null, null, List.of("x"), 10),
                new AiUnit("same", UnitType.THEORY, "B", "Detailed safe content body", null, null, List.of("x"), 10),
                new AiUnit("c", UnitType.THEORY, "C", "Detailed safe content body", null, null, List.of("x"), 10),
                new AiUnit("d", UnitType.THEORY, "D", "Detailed safe content body", null, null, List.of("x"), 10)),
                "Long enough conclusion", "model", "study-material-v1");
        assertThatThrownBy(() -> service.persistDraft(jobId, topicId, unsafe)).isInstanceOf(ApiException.class);
        service.generate(jobId);
        assertThat(service.job(jobId).status()).isEqualTo(StudyModels.JobStatus.FAILED);
    }

    @Test
    void serializesAiGenerationContractAsCamelCaseJson() throws Exception {
        AiRequest request = new AiRequest(UUID.randomUUID(), "study-material-v1",
                new AiTopic(UUID.randomUUID(), "Java", "Java", "OOPS", "Four pillars",
                        SkillLevel.BEGINNER, 60, List.of("Learn with examples")));

        JsonNode payload = json.readTree(json.writeValueAsString(request));

        assertThat(payload.has("jobId")).isTrue();
        assertThat(payload.has("promptVersion")).isTrue();
        assertThat(payload.get("topic").has("skillLevel")).isTrue();
        assertThat(payload.get("topic").has("estimatedMinutes")).isTrue();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("admin")).authorities(() -> "ROLE_ADMIN");
    }
    private org.springframework.test.web.servlet.request.RequestPostProcessor candidate() {
        return jwt().jwt(j -> j.subject("candidate")).authorities(() -> "ROLE_CANDIDATE");
    }
    private UUID id(MvcResult result) throws Exception {
        JsonNode node = json.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(node.get("id").asText());
    }
    private static AiContent validContent(UUID topic, String title) {
        return new AiContent(topic, title, "A detailed introduction for the learner", List.of(
                unit("overview", UnitType.OVERVIEW), unit("theory", UnitType.THEORY),
                unit("example", UnitType.EXAMPLE), unit("summary", UnitType.SUMMARY)),
                "A detailed conclusion for the learner", "openai/gpt-5-mini", "study-material-v1");
    }
    private static AiUnit unit(String key, UnitType type) {
        return new AiUnit(key, type, key, "Detailed study material for " + key, null, null, List.of("Remember " + key), 15);
    }
    private static AiContent contentWithUnits(UUID topic, int count, boolean duplicate, boolean unsafe) {
        java.util.ArrayList<AiUnit> units = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            String key = duplicate && index == count - 1 ? "unit-0" : "unit-" + index;
            String body = unsafe && index == count - 1 ? "<script>unsafe</script>" : "Detailed safe study material";
            units.add(new AiUnit(key, UnitType.THEORY, "Unit " + index, body, null, null, List.of("Remember"), 10));
        }
        return new AiContent(topic, "Coverage", "Long introduction", units, "Long conclusion", "model", "prompt");
    }
}
