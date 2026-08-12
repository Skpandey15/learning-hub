package com.learninghub.security;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class AccessPolicy {
    @Id private Short id;
    private boolean sharedStudyAccess;
    private boolean candidateEnabled;
    private boolean interviewerEnabled;
    private Instant updatedAt;
    private String updatedBy;

    protected AccessPolicy() {}
    AccessPolicy(boolean shared, boolean candidate, boolean interviewer, String actor) {
        id = 1; update(shared, candidate, interviewer, actor);
    }
    public boolean isSharedStudyAccess() { return sharedStudyAccess; }
    public boolean isCandidateEnabled() { return candidateEnabled; }
    public boolean isInterviewerEnabled() { return interviewerEnabled; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void update(boolean shared, boolean candidate, boolean interviewer, String actor) {
        sharedStudyAccess = shared; candidateEnabled = candidate; interviewerEnabled = interviewer;
        updatedAt = Instant.now(); updatedBy = actor;
    }
}
