package com.example.featureflag.api;

import com.example.featureflag.api.dto.Dtos.AuditLogResponse;
import com.example.featureflag.application.AuditService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/flags/{flagKey}/audit")
    public List<AuditLogResponse> getAuditHistory(@PathVariable String flagKey) {
        return auditService.getAuditHistory(flagKey).stream()
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getActor(),
                        log.getAction(),
                        log.getResourceType(),
                        log.getResourceKey(),
                        log.getBeforeJson(),
                        log.getAfterJson(),
                        log.getCreatedAt()
                ))
                .toList();
    }
}
