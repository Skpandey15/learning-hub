package com.learninghub.study;

import com.learninghub.security.AccessPolicyService;
import com.learninghub.shared.error.ApiException;
import com.learninghub.shared.error.ErrorCode;
import com.learninghub.shared.web.CorrelationIdFilter;
import com.learninghub.study.StudyModels.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Validated
public class StudyPlatformController {
    private final StudyPlatformService service;
    private final AccessPolicyService policy;
    StudyPlatformController(StudyPlatformService service, AccessPolicyService policy) { this.service = service; this.policy = policy; }

    @GetMapping("/learning/domains")
    List<DomainView> domains(Authentication auth) { requireStudy(auth); return service.domains(false); }
    @GetMapping("/learning/domains/{domainId}/technologies")
    List<TechnologyView> technologies(@PathVariable UUID domainId, Authentication auth) { requireStudy(auth); return service.technologies(domainId, false); }
    @GetMapping("/learning/technologies/{technologyId}/topics")
    List<TopicView> topics(@PathVariable UUID technologyId, Authentication auth) { requireStudy(auth); return service.topics(technologyId, false); }
    @GetMapping("/learning/topics/{topicId}/content")
    ContentView content(@PathVariable UUID topicId, Authentication auth) { requireStudy(auth); return service.currentContent(topicId, auth.getName()); }
    @PutMapping("/learning/units/{unitId}/completion")
    CompletionView completion(@PathVariable UUID unitId, @Valid @RequestBody CompletionRequest request, Authentication auth) {
        requireStudy(auth); return service.complete(unitId, request.completed(), auth.getName());
    }
    @GetMapping("/learning/progress/me")
    ProgressView progress(Authentication auth) { requireStudy(auth); return service.progress(auth.getName()); }

    @GetMapping("/admin/ecosystems")
    List<DomainView> adminDomains() { return service.domains(true); }
    @PostMapping("/admin/ecosystems") @ResponseStatus(HttpStatus.CREATED)
    DomainView createDomain(@Valid @RequestBody CreateDomain body, Authentication auth, HttpServletRequest request) {
        return service.createDomain(body, auth.getName(), correlation(request));
    }
    @PostMapping("/admin/ecosystems/{domainId}/technologies") @ResponseStatus(HttpStatus.CREATED)
    TechnologyView createTechnology(@PathVariable UUID domainId, @Valid @RequestBody CreateTechnology body,
            Authentication auth, HttpServletRequest request) {
        return service.createTechnology(domainId, body, auth.getName(), correlation(request));
    }
    @PostMapping("/admin/technologies/{technologyId}/topics") @ResponseStatus(HttpStatus.CREATED)
    TopicView createTopic(@PathVariable UUID technologyId, @Valid @RequestBody CreateTopic body,
            Authentication auth, HttpServletRequest request) {
        return service.createTopic(technologyId, body, auth.getName(), correlation(request));
    }
    @PostMapping("/admin/ecosystems/{domainId}/publish")
    DomainView publishDomain(@PathVariable UUID domainId, @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody ActionReason reason, Authentication auth, HttpServletRequest request) {
        return service.publishDomain(domainId, version(ifMatch), auth.getName(), correlation(request), reason.reason());
    }
    @PostMapping("/admin/topics/{topicId}/generation-jobs") @ResponseStatus(HttpStatus.ACCEPTED)
    GenerationJobView generate(@PathVariable UUID topicId, @RequestHeader("Idempotency-Key") @NotBlank String key,
            Authentication auth, HttpServletRequest request) {
        return service.requestGeneration(topicId, auth.getName(), key, correlation(request));
    }
    @GetMapping("/admin/generation-jobs/{jobId}")
    GenerationJobView job(@PathVariable UUID jobId) { return service.job(jobId); }
    @GetMapping("/admin/content-versions/{versionId}")
    ContentView draft(@PathVariable UUID versionId, Authentication auth) { return service.draftContent(versionId, auth.getName()); }
    @PostMapping("/admin/content-versions/{versionId}/publish")
    ContentView publishContent(@PathVariable UUID versionId, @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody ActionReason reason, Authentication auth, HttpServletRequest request) {
        return service.publishContent(versionId, version(ifMatch), auth.getName(), correlation(request), reason.reason());
    }

    private void requireStudy(Authentication auth) { policy.requireStudyAccess(roles(auth)); }
    private static Set<String> roles(Authentication auth) { return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet()); }
    private static String correlation(HttpServletRequest request) { Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE); return value == null ? "unknown" : value.toString(); }
    private static long version(String value) { try { return Long.parseLong(value.replace("\"", "")); } catch (NumberFormatException e) { throw new ApiException(ErrorCode.MALFORMED_REQUEST); } }
    record CompletionRequest(boolean completed) {}
}
