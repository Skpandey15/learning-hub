package com.learninghub.security;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class SecurityAuditEvent {
    @Id private UUID id;
    private Instant occurredAt;
    private String actorId;
    private String actorRoles;
    private String action;
    private String resource;
    private String outcome;
    private String correlationId;
    private String sourceIpHash;
    private String details;
    protected SecurityAuditEvent() {}
    SecurityAuditEvent(String actorId, String roles, String action, String resource, String outcome,
            String correlationId, String sourceIpHash, String details) {
        id = UUID.randomUUID(); occurredAt = Instant.now(); this.actorId = actorId; actorRoles = roles;
        this.action = action; this.resource = resource; this.outcome = outcome;
        this.correlationId = correlationId; this.sourceIpHash = sourceIpHash; this.details = details;
    }
}
