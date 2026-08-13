package com.learninghub.study;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class StudyModels {
    private StudyModels() {}

    public record CreateDomain(
            @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 80) String slug,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 4000) String description,
            @Min(1) int displayOrder) {}
    public record CreateTechnology(
            @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 80) String slug,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 4000) String description,
            @Min(1) int displayOrder) {}
    public record CreateTopic(
            @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 100) String slug,
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 4000) String summary,
            @NotNull SkillLevel skillLevel,
            @Min(5) @Max(1440) int estimatedMinutes,
            @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 300) String> objectives,
            @Min(1) int displayOrder) {}
    public record ActionReason(@NotBlank @Size(max = 500) String reason) {}
    public record DomainView(UUID id, String slug, String name, String description, int displayOrder,
                             LifecycleStatus status, long version) {}
    public record TechnologyView(UUID id, UUID domainId, String slug, String name, String description,
                                 int displayOrder, LifecycleStatus status, long version) {}
    public record TopicView(UUID id, UUID technologyId, String slug, String title, String summary,
                            SkillLevel skillLevel, int estimatedMinutes, List<String> objectives,
                            int displayOrder, LifecycleStatus status, UUID currentContentVersionId, long version) {}
    public record GenerationJobView(UUID id, UUID topicId, JobStatus status, Instant requestedAt,
                                    Instant completedAt, UUID resultVersionId, String errorCode) {}
    public record ContentView(UUID versionId, UUID topicId, int versionNumber, String title,
                              String introduction, String conclusion, String modelName,
                              String promptVersion, List<UnitView> units) {}
    public record UnitView(UUID id, String stableKey, UnitType type, String title, String bodyMarkdown,
                           String codeLanguage, String codeExample, List<String> keyTakeaways,
                           int displayOrder, int estimatedMinutes, boolean completed) {}
    public record CompletionView(UUID unitId, boolean completed, Instant completedAt) {}
    public record ProgressView(long completedUnits, long totalUnits, double percent) {}
    public record AiRequest(UUID jobId, String promptVersion, @Valid AiTopic topic) {}
    public record AiTopic(UUID id, String domain, String technology, String title, String summary,
                          SkillLevel skillLevel, int estimatedMinutes, List<String> objectives) {}
    public record AiContent(UUID topicId, String title, String introduction,
                            @Size(min = 4, max = 12) List<@Valid AiUnit> units, String conclusion,
                            String modelName, String promptVersion) {}
    public record AiUnit(String stableKey, UnitType type, String title, String bodyMarkdown,
                         String codeLanguage, String codeExample, List<String> keyTakeaways,
                         int estimatedMinutes) {}

    public enum LifecycleStatus { DRAFT, PUBLISHED, ARCHIVED }
    public enum SkillLevel { BEGINNER, INTERMEDIATE, ADVANCED }
    public enum JobStatus { QUEUED, GENERATING, VALIDATING, AWAITING_REVIEW, PUBLISHED, REJECTED, FAILED }
    public enum UnitType { OVERVIEW, THEORY, EXAMPLE, EXERCISE, SUMMARY }
}
