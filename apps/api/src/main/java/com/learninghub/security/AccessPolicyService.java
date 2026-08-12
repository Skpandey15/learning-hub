package com.learninghub.security;

import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessPolicyService {
    private static final short POLICY_ID = 1;
    private final AccessPolicyRepository repository;
    AccessPolicyService(AccessPolicyRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public AccessPolicy current() { return repository.findById(POLICY_ID).orElseThrow(); }

    @Transactional(readOnly = true)
    public void requireStudyAccess(Set<String> authorities) {
        if (authorities.contains(LearningRole.ADMIN.authority())) return;
        AccessPolicy policy = current();
        boolean allowed = (authorities.contains(LearningRole.CANDIDATE.authority()) && policy.isCandidateEnabled())
                || (authorities.contains(LearningRole.INTERVIEWER.authority()) && policy.isInterviewerEnabled());
        if (!allowed) throw new AccessDeniedException("Study access is disabled for this role");
    }

    @Transactional
    public AccessPolicy update(boolean shared, boolean candidate, boolean interviewer, String actor) {
        AccessPolicy policy = current();
        policy.update(shared, candidate, interviewer, actor);
        return policy;
    }
}
