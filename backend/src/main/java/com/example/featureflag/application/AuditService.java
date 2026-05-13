package com.example.featureflag.application;

import com.example.featureflag.domain.AuditLogEntity;
import com.example.featureflag.infrastructure.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String actor, String action, String resourceType, String resourceKey, String beforeJson, String afterJson) {
        AuditLogEntity log = new AuditLogEntity();
        log.setActor(actor == null || actor.isBlank() ? "demo-user" : actor);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceKey(resourceKey);
        log.setBeforeJson(beforeJson);
        log.setAfterJson(afterJson);
        auditLogRepository.save(log);
    }
}
