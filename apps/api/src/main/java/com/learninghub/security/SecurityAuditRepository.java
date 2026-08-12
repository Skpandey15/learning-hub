package com.learninghub.security;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SecurityAuditRepository extends JpaRepository<SecurityAuditEvent, UUID> {}
