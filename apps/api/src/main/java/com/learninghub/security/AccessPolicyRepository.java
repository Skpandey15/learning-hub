package com.learninghub.security;

import org.springframework.data.jpa.repository.JpaRepository;

interface AccessPolicyRepository extends JpaRepository<AccessPolicy, Short> {}
