package com.learninghub.study;

import com.learninghub.shared.error.ApiException;
import com.learninghub.shared.error.ErrorCode;
import com.learninghub.study.StudyModels.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class StudyPlatformService {
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {};
    private final JdbcClient jdbc;
    private final ObjectMapper json;
    private final RestClient ai;
    private final String serviceToken;
    private final String model;
    private final String promptVersion;

    StudyPlatformService(JdbcClient jdbc, ObjectMapper json,
            @Value("${AI_SERVICE_BASE_URL:http://localhost:8000}") String aiBaseUrl,
            @Value("${INTERNAL_AI_SERVICE_TOKEN:local-development-token-must-be-32-chars}") String serviceToken,
            @Value("${STUDY_MODEL:openai/gpt-5-mini}") String model,
            @Value("${PROMPT_VERSION:study-material-v1}") String promptVersion) {
        this.jdbc = jdbc; this.json = json; this.ai = RestClient.builder().baseUrl(aiBaseUrl).build();
        this.serviceToken = serviceToken; this.model = model; this.promptVersion = promptVersion;
    }

    @Transactional
    public DomainView createDomain(CreateDomain request, String actor, String correlationId) {
        UUID id = UUID.randomUUID(); OffsetDateTime now = now();
        jdbc.sql("""
INSERT INTO learning_domain(id,slug,name,description,display_order,lifecycle_status,created_by,updated_by,created_at,updated_at)
                VALUES(:id,:slug,:name,:description,:displayOrder,'DRAFT',:actor,:actor,:now,:now)""")
                .param("id", id).param("slug", request.slug()).param("name", request.name())
                .param("description", request.description()).param("displayOrder", request.displayOrder())
                .param("actor", actor).param("now", now).update();
        audit(actor, "DOMAIN_CREATE", "learning_domain", id, correlationId, "ALLOWED", null);
        return domain(id);
    }

    @Transactional
    public TechnologyView createTechnology(UUID domainId, CreateTechnology request, String actor, String correlationId) {
        requireExists("learning_domain", domainId); UUID id = UUID.randomUUID(); OffsetDateTime now = now();
        jdbc.sql("""
INSERT INTO learning_technology(id,domain_id,slug,name,description,display_order,lifecycle_status,created_by,updated_by,created_at,updated_at)
                VALUES(:id,:parent,:slug,:name,:description,:displayOrder,'DRAFT',:actor,:actor,:now,:now)""")
                .param("id", id).param("parent", domainId).param("slug", request.slug()).param("name", request.name())
                .param("description", request.description()).param("displayOrder", request.displayOrder())
                .param("actor", actor).param("now", now).update();
        audit(actor, "TECHNOLOGY_CREATE", "learning_technology", id, correlationId, "ALLOWED", null);
        return technology(id);
    }

    @Transactional
    public TopicView createTopic(UUID technologyId, CreateTopic request, String actor, String correlationId) {
        requireExists("learning_technology", technologyId); UUID id = UUID.randomUUID(); OffsetDateTime now = now();
        jdbc.sql("""
INSERT INTO learning_topic(id,technology_id,slug,title,summary,skill_level,estimated_minutes,objectives,display_order,lifecycle_status,created_by,updated_by,created_at,updated_at)
                VALUES(:id,:parent,:slug,:title,:summary,:level,:minutes,CAST(:objectives AS jsonb),:displayOrder,'DRAFT',:actor,:actor,:now,:now)""")
                .param("id", id).param("parent", technologyId).param("slug", request.slug()).param("title", request.title())
                .param("summary", request.summary()).param("level", request.skillLevel().name()).param("minutes", request.estimatedMinutes())
                .param("objectives", write(request.objectives())).param("displayOrder", request.displayOrder())
                .param("actor", actor).param("now", now).update();
        audit(actor, "TOPIC_CREATE", "learning_topic", id, correlationId, "ALLOWED", null);
        return topic(id, false);
    }

    @Transactional
    public DomainView publishDomain(UUID id, long expectedVersion, String actor, String correlationId, String reason) {
        long topics = jdbc.sql("""
SELECT count(*) FROM learning_topic t JOIN learning_technology x ON x.id=t.technology_id
                WHERE x.domain_id=:id""").param("id", id).query(Long.class).single();
        if (topics == 0) throw new ApiException(ErrorCode.VALIDATION_FAILED, Map.of("reason", "Ecosystem requires at least one topic"));
        int changed = jdbc.sql("""
UPDATE learning_domain SET lifecycle_status='PUBLISHED',version=version+1,updated_by=:actor,updated_at=:now
                WHERE id=:id AND version=:version AND lifecycle_status='DRAFT'""").param("actor", actor).param("now", now())
                .param("id", id).param("version", expectedVersion).update();
        if (changed != 1) throw new ApiException(ErrorCode.CONFLICT);
        jdbc.sql("UPDATE learning_technology SET lifecycle_status='PUBLISHED',version=version+1,updated_by=:actor,updated_at=:now WHERE domain_id=:id AND lifecycle_status='DRAFT'")
                .param("actor", actor).param("now", now()).param("id", id).update();
        jdbc.sql("""
UPDATE learning_topic SET lifecycle_status='PUBLISHED',version=version+1,updated_by=:actor,updated_at=:now
                WHERE technology_id IN (SELECT id FROM learning_technology WHERE domain_id=:id) AND lifecycle_status='DRAFT'""")
                .param("actor", actor).param("now", now()).param("id", id).update();
        audit(actor, "DOMAIN_PUBLISH", "learning_domain", id, correlationId, "ALLOWED", reason);
        return domain(id);
    }

    public List<DomainView> domains(boolean admin) {
        String filter = admin ? "" : " WHERE lifecycle_status='PUBLISHED' AND active=true";
        return jdbc.sql("SELECT * FROM learning_domain" + filter + " ORDER BY display_order").query(this::mapDomain).list();
    }
    public List<TechnologyView> technologies(UUID domainId, boolean admin) {
        String filter = admin ? "" : " AND lifecycle_status='PUBLISHED' AND active=true";
        return jdbc.sql("SELECT * FROM learning_technology WHERE domain_id=:id" + filter + " ORDER BY display_order")
                .param("id", domainId).query(this::mapTechnology).list();
    }
    public List<TopicView> topics(UUID technologyId, boolean admin) {
        String filter = admin ? "" : " AND lifecycle_status='PUBLISHED' AND active=true";
        return jdbc.sql("SELECT * FROM learning_topic WHERE technology_id=:id" + filter + " ORDER BY display_order")
                .param("id", technologyId).query(this::mapTopic).list();
    }

    @Transactional
    public GenerationJobView requestGeneration(UUID topicId, String actor, String idempotencyKey, String correlationId) {
        TopicView topic = topic(topicId, false);
        if (topic.status() != LifecycleStatus.PUBLISHED) throw new ApiException(ErrorCode.CONFLICT);
        var existing = jdbc.sql("SELECT * FROM content_generation_job WHERE requested_by_subject=:actor AND idempotency_key=:key")
                .param("actor", actor).param("key", idempotencyKey).query(this::mapJob).optional();
        if (existing.isPresent()) return existing.get();
        UUID id = UUID.randomUUID(); OffsetDateTime now = now();
        jdbc.sql("""
INSERT INTO content_generation_job(id,topic_id,requested_by_subject,status,requested_at,prompt_version,model_name,correlation_id,idempotency_key)
                VALUES(:id,:topic,:actor,'QUEUED',:now,:prompt,:model,:correlation,:key)""")
                .param("id", id).param("topic", topicId).param("actor", actor).param("now", now)
                .param("prompt", promptVersion).param("model", model).param("correlation", correlationId).param("key", idempotencyKey).update();
        audit(actor, "CONTENT_GENERATE_REQUEST", "learning_topic", topicId, correlationId, "ALLOWED", null);
        return job(id);
    }

    public GenerationJobView job(UUID id) {
        return jdbc.sql("SELECT * FROM content_generation_job WHERE id=:id").param("id", id).query(this::mapJob)
                .optional().orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public UUID claimNextJob() {
        var id = jdbc.sql("SELECT id FROM content_generation_job WHERE status='QUEUED' ORDER BY requested_at FOR UPDATE SKIP LOCKED LIMIT 1")
                .query(UUID.class).optional();
        if (id.isEmpty()) return null;
        jdbc.sql("UPDATE content_generation_job SET status='GENERATING',started_at=:now,heartbeat_at=:now,attempt_count=attempt_count+1 WHERE id=:id")
                .param("now", now()).param("id", id.get()).update(); return id.get();
    }

    public void generate(UUID jobId) {
        try {
            AiRequest request = generationRequest(jobId);
            AiContent content = ai.post().uri("/internal/v1/study-content/generate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken)
                    .header("Idempotency-Key", jobId.toString()).header("X-Correlation-ID", job(jobId).id().toString())
                    .body(request).retrieve().body(AiContent.class);
            if (content == null) throw new IllegalStateException("Empty AI response");
            persistDraft(jobId, request.topic().id(), content);
        } catch (Exception exception) {
            failJob(jobId, "AI_GENERATION_FAILED");
        }
    }

    @Transactional
    void persistDraft(UUID jobId, UUID topicId, AiContent content) {
        validateContent(topicId, content);
        jdbc.sql("UPDATE content_generation_job SET status='VALIDATING',heartbeat_at=:now WHERE id=:id")
                .param("now", now()).param("id", jobId).update();
        int versionNumber = jdbc.sql("SELECT COALESCE(max(version_number),0)+1 FROM study_content_version WHERE topic_id=:id")
                .param("id", topicId).query(Integer.class).single();
        UUID versionId = UUID.randomUUID(); OffsetDateTime now = now(); String canonical = write(content);
        jdbc.sql("""
INSERT INTO study_content_version(id,topic_id,version_number,status,title,introduction,conclusion,model_name,prompt_version,content_hash,generated_at,created_at)
                VALUES(:id,:topic,:number,'DRAFT',:title,:intro,:conclusion,:model,:prompt,:hash,:now,:now)""")
                .param("id", versionId).param("topic", topicId).param("number", versionNumber).param("title", content.title())
                .param("intro", content.introduction()).param("conclusion", content.conclusion()).param("model", content.modelName())
                .param("prompt", content.promptVersion()).param("hash", sha256(canonical)).param("now", now).update();
        int order = 1;
        for (AiUnit unit : content.units()) {
            jdbc.sql("""
INSERT INTO study_unit(id,content_version_id,stable_key,unit_type,title,body_markdown,code_language,code_example,key_takeaways,display_order,estimated_minutes,created_at)
                    VALUES(:id,:version,:key,:type,:title,:body,:language,:code,CAST(:takeaways AS jsonb),:ordering,:minutes,:now)""")
                    .param("id", UUID.randomUUID()).param("version", versionId).param("key", unit.stableKey()).param("type", unit.type().name())
                    .param("title", unit.title()).param("body", unit.bodyMarkdown()).param("language", unit.codeLanguage()).param("code", unit.codeExample())
                    .param("takeaways", write(unit.keyTakeaways())).param("ordering", order++).param("minutes", unit.estimatedMinutes()).param("now", now).update();
        }
        jdbc.sql("UPDATE content_generation_job SET status='AWAITING_REVIEW',completed_at=:now,result_version_id=:version WHERE id=:id")
                .param("now", now).param("version", versionId).param("id", jobId).update();
    }

    @Transactional
    public ContentView publishContent(UUID versionId, long expectedVersion, String actor, String correlationId, String reason) {
        UUID topicId = jdbc.sql("SELECT topic_id FROM study_content_version WHERE id=:id AND status='DRAFT'").param("id", versionId)
                .query(UUID.class).optional().orElseThrow(() -> new ApiException(ErrorCode.CONFLICT));
        jdbc.sql("UPDATE study_content_version SET status='SUPERSEDED' WHERE topic_id=:topic AND status='PUBLISHED'")
                .param("topic", topicId).update();
        int changed = jdbc.sql("""
UPDATE study_content_version SET status='PUBLISHED',published_at=:now,reviewed_by=:actor,publication_reason=:reason,version=version+1
                WHERE id=:id AND version=:version AND status='DRAFT'""").param("now", now()).param("actor", actor)
                .param("reason", reason).param("id", versionId).param("version", expectedVersion).update();
        if (changed != 1) throw new ApiException(ErrorCode.CONFLICT);
        jdbc.sql("UPDATE learning_topic SET current_content_version_id=:version,version=version+1,updated_at=:now,updated_by=:actor WHERE id=:topic")
                .param("version", versionId).param("now", now()).param("actor", actor).param("topic", topicId).update();
        jdbc.sql("UPDATE content_generation_job SET status='PUBLISHED' WHERE result_version_id=:version")
                .param("version", versionId).update();
        audit(actor, "CONTENT_PUBLISH", "study_content_version", versionId, correlationId, "ALLOWED", reason);
        return content(versionId, actor);
    }

    public ContentView currentContent(UUID topicId, String subject) {
        UUID versionId = jdbc.sql("SELECT current_content_version_id FROM learning_topic WHERE id=:id AND lifecycle_status='PUBLISHED' AND active=true AND current_content_version_id IS NOT NULL")
                .param("id", topicId).query(UUID.class).optional().orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        return content(versionId, subject);
    }

    public ContentView draftContent(UUID versionId, String subject) { return content(versionId, subject); }

    @Transactional
    public CompletionView complete(UUID unitId, boolean completed, String subject) {
        requireExists("study_unit", unitId); OffsetDateTime now = now();
        jdbc.sql("""
INSERT INTO user_unit_progress(user_subject,unit_id,completed,completed_at,updated_at)
                VALUES(:subject,:unit,:completed,:completedAt,:now)
                ON CONFLICT(user_subject,unit_id) DO UPDATE SET completed=EXCLUDED.completed,completed_at=EXCLUDED.completed_at,updated_at=EXCLUDED.updated_at""")
                .param("subject", subject).param("unit", unitId).param("completed", completed)
                .param("completedAt", completed ? now : null).param("now", now).update();
        return new CompletionView(unitId, completed, completed ? now.toInstant() : null);
    }

    public ProgressView progress(String subject) {
        Map<String, Object> row = jdbc.sql("""
SELECT count(*) AS total, count(*) FILTER (WHERE p.completed=true) AS completed
                FROM study_unit u JOIN study_content_version v ON v.id=u.content_version_id
                JOIN learning_topic t ON t.current_content_version_id=v.id
                LEFT JOIN user_unit_progress p ON p.unit_id=u.id AND p.user_subject=:subject WHERE v.status='PUBLISHED'""")
                .param("subject", subject).query().singleRow();
        long total = ((Number) row.get("total")).longValue(); long done = ((Number) row.get("completed")).longValue();
        return new ProgressView(done, total, total == 0 ? 0 : Math.round(done * 10000.0 / total) / 100.0);
    }

    private AiRequest generationRequest(UUID jobId) {
        return jdbc.sql("""
SELECT t.id,d.name domain,x.name technology,t.title,t.summary,t.skill_level,t.estimated_minutes,t.objectives,j.prompt_version
                FROM content_generation_job j JOIN learning_topic t ON t.id=j.topic_id JOIN learning_technology x ON x.id=t.technology_id
                JOIN learning_domain d ON d.id=x.domain_id WHERE j.id=:id""").param("id", jobId).query((rs, n) -> new AiRequest(jobId,
                rs.getString("prompt_version"), new AiTopic(rs.getObject("id", UUID.class), rs.getString("domain"), rs.getString("technology"),
                rs.getString("title"), rs.getString("summary"), SkillLevel.valueOf(rs.getString("skill_level")), rs.getInt("estimated_minutes"),
                readStrings(rs.getString("objectives"))))).single();
    }
    private void validateContent(UUID topicId, AiContent content) {
        if (!topicId.equals(content.topicId()) || content.units() == null || content.units().size() < 4 || content.units().size() > 12)
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        long keys = content.units().stream().map(AiUnit::stableKey).distinct().count();
        if (keys != content.units().size() || content.units().stream().anyMatch(u -> u.bodyMarkdown().contains("<script")))
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
    }
    private void failJob(UUID id, String code) {
        jdbc.sql("UPDATE content_generation_job SET status='FAILED',completed_at=:now,error_code=:code,error_message='Generation failed safely' WHERE id=:id")
                .param("now", now()).param("code", code).param("id", id).update();
    }
    private ContentView content(UUID id, String subject) {
        var headers = jdbc.sql("SELECT * FROM study_content_version WHERE id=:id").param("id", id).query().listOfRows();
        if (headers.isEmpty()) throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        var header = headers.getFirst();
        List<UnitView> units = jdbc.sql("""
SELECT u.*,COALESCE(p.completed,false) completed FROM study_unit u
                LEFT JOIN user_unit_progress p ON p.unit_id=u.id AND p.user_subject=:subject WHERE u.content_version_id=:id ORDER BY u.display_order""")
                .param("subject", subject).param("id", id).query((rs, n) -> new UnitView(rs.getObject("id", UUID.class), rs.getString("stable_key"),
                UnitType.valueOf(rs.getString("unit_type")), rs.getString("title"), rs.getString("body_markdown"), rs.getString("code_language"),
                rs.getString("code_example"), readStrings(rs.getString("key_takeaways")), rs.getInt("display_order"), rs.getInt("estimated_minutes"),
                rs.getBoolean("completed"))).list();
        return new ContentView(id, (UUID) header.get("topic_id"), ((Number) header.get("version_number")).intValue(),
                (String) header.get("title"), (String) header.get("introduction"), (String) header.get("conclusion"),
                (String) header.get("model_name"), (String) header.get("prompt_version"), units);
    }
    private DomainView domain(UUID id) { return jdbc.sql("SELECT * FROM learning_domain WHERE id=:id")
            .param("id", id).query(this::mapDomain).optional().orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private TechnologyView technology(UUID id) { return jdbc.sql("SELECT * FROM learning_technology WHERE id=:id")
            .param("id", id).query(this::mapTechnology).optional().orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private TopicView topic(UUID id, boolean publishedOnly) { return jdbc.sql("SELECT * FROM learning_topic WHERE id=:id" + (publishedOnly ? " AND lifecycle_status='PUBLISHED'" : ""))
            .param("id", id).query(this::mapTopic).optional().orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private DomainView mapDomain(java.sql.ResultSet rs, int n) throws java.sql.SQLException { return new DomainView(rs.getObject("id", UUID.class),rs.getString("slug"),rs.getString("name"),rs.getString("description"),rs.getInt("display_order"),LifecycleStatus.valueOf(rs.getString("lifecycle_status")),rs.getLong("version")); }
    private TechnologyView mapTechnology(java.sql.ResultSet rs, int n) throws java.sql.SQLException { return new TechnologyView(rs.getObject("id", UUID.class),rs.getObject("domain_id", UUID.class),rs.getString("slug"),rs.getString("name"),rs.getString("description"),rs.getInt("display_order"),LifecycleStatus.valueOf(rs.getString("lifecycle_status")),rs.getLong("version")); }
    private TopicView mapTopic(java.sql.ResultSet rs, int n) throws java.sql.SQLException { return new TopicView(rs.getObject("id", UUID.class),rs.getObject("technology_id", UUID.class),rs.getString("slug"),rs.getString("title"),rs.getString("summary"),SkillLevel.valueOf(rs.getString("skill_level")),rs.getInt("estimated_minutes"),readStrings(rs.getString("objectives")),rs.getInt("display_order"),LifecycleStatus.valueOf(rs.getString("lifecycle_status")),rs.getObject("current_content_version_id", UUID.class),rs.getLong("version")); }
    private GenerationJobView mapJob(java.sql.ResultSet rs, int n) throws java.sql.SQLException { return new GenerationJobView(rs.getObject("id", UUID.class),rs.getObject("topic_id", UUID.class),JobStatus.valueOf(rs.getString("status")),rs.getTimestamp("requested_at").toInstant(),rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),rs.getObject("result_version_id", UUID.class),rs.getString("error_code")); }
    private void requireExists(String table, UUID id) { Long count = jdbc.sql("SELECT count(*) FROM " + table + " WHERE id=:id").param("id", id).query(Long.class).single(); if (count != 1) throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND); }
    private void audit(String actor,String action,String type,UUID target,String correlation,String outcome,String reason) { jdbc.sql("INSERT INTO admin_audit_event(id,occurred_at,actor_subject,action,target_type,target_id,correlation_id,outcome,reason) VALUES(:id,:now,:actor,:action,:type,:target,:correlation,:outcome,:reason)").param("id",UUID.randomUUID()).param("now",now()).param("actor",actor).param("action",action).param("type",type).param("target",target).param("correlation",correlation).param("outcome",outcome).param("reason",reason).update(); }
    private static OffsetDateTime now() { return OffsetDateTime.now(ZoneOffset.UTC); }
    private String write(Object value) { try { return json.writeValueAsString(value); } catch (JacksonException e) { throw new ApiException(ErrorCode.INTERNAL_ERROR); } }
    private List<String> readStrings(String value) { try { return json.readValue(value, STRINGS); } catch (JacksonException e) { throw new ApiException(ErrorCode.INTERNAL_ERROR); } }
    private static String sha256(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
}
