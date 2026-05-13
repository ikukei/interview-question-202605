package com.example.featureflag.application;

import com.example.featureflag.api.dto.Dtos.ConditionRequest;
import com.example.featureflag.api.dto.Dtos.PublishRequest;
import com.example.featureflag.api.dto.Dtos.SnapshotResponse;
import com.example.featureflag.application.model.SnapshotModels.Snapshot;
import com.example.featureflag.application.model.SnapshotModels.SnapshotCondition;
import com.example.featureflag.application.model.SnapshotModels.SnapshotFlag;
import com.example.featureflag.application.model.SnapshotModels.SnapshotRule;
import com.example.featureflag.domain.ConfigSnapshotEntity;
import com.example.featureflag.domain.FlagEntity;
import com.example.featureflag.domain.RuleEntity;
import com.example.featureflag.infrastructure.repository.ConfigSnapshotRepository;
import com.example.featureflag.infrastructure.repository.FlagRepository;
import com.example.featureflag.infrastructure.repository.RuleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublishService {
    private final FlagRepository flagRepository;
    private final RuleRepository ruleRepository;
    private final ConfigSnapshotRepository snapshotRepository;
    private final SnapshotCache snapshotCache;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public PublishService(
            FlagRepository flagRepository,
            RuleRepository ruleRepository,
            ConfigSnapshotRepository snapshotRepository,
            SnapshotCache snapshotCache,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.flagRepository = flagRepository;
        this.ruleRepository = ruleRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotCache = snapshotCache;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public SnapshotResponse publish(PublishRequest request) {
        long nextVersion = snapshotRepository
                .findTopByAppKeyAndEnvironmentOrderByVersionDesc(request.appKey(), request.environment())
                .map(ConfigSnapshotEntity::getVersion)
                .orElse(0L) + 1L;

        List<SnapshotFlag> flags = flagRepository
                .findByAppKeyAndEnvironmentOrderByFlagKeyAsc(request.appKey(), request.environment())
                .stream()
                .filter(flag -> !"archived".equalsIgnoreCase(flag.getStatus()))
                .map(this::toSnapshotFlag)
                .toList();

        Snapshot withoutChecksum = new Snapshot(request.appKey(), request.environment(), nextVersion, "", flags);
        String checksum = checksum(writeJson(withoutChecksum));
        Snapshot snapshot = new Snapshot(request.appKey(), request.environment(), nextVersion, checksum, flags);

        ConfigSnapshotEntity entity = new ConfigSnapshotEntity();
        entity.setAppKey(request.appKey());
        entity.setEnvironment(request.environment());
        entity.setVersion(nextVersion);
        entity.setChecksum(checksum);
        entity.setSnapshotJson(writeJson(snapshot));
        entity.setPublishedBy(request.actor() == null || request.actor().isBlank() ? "demo-user" : request.actor());
        ConfigSnapshotEntity saved = snapshotRepository.save(entity);

        snapshotCache.put(snapshot);
        auditService.record(saved.getPublishedBy(), "publish", "snapshot", request.environment() + ":" + request.appKey(), null, saved.getSnapshotJson());
        return new SnapshotResponse(saved.getAppKey(), saved.getEnvironment(), saved.getVersion(), saved.getChecksum(), saved.getPublishedAt());
    }

    public SnapshotResponse latest(String appKey, String environment) {
        ConfigSnapshotEntity latest = snapshotRepository.findTopByAppKeyAndEnvironmentOrderByVersionDesc(appKey, environment)
                .orElseThrow(() -> new NotFoundException("No snapshot published for scope"));
        return new SnapshotResponse(latest.getAppKey(), latest.getEnvironment(), latest.getVersion(), latest.getChecksum(), latest.getPublishedAt());
    }

    public Snapshot loadLatestSnapshot(String appKey, String environment) {
        return snapshotCache.get(appKey, environment)
                .orElseGet(() -> {
                    ConfigSnapshotEntity latest = snapshotRepository.findTopByAppKeyAndEnvironmentOrderByVersionDesc(appKey, environment)
                            .orElseThrow(() -> new NotFoundException("No snapshot published for scope"));
                    Snapshot snapshot = readSnapshot(latest.getSnapshotJson());
                    snapshotCache.put(snapshot);
                    return snapshot;
                });
    }

    private SnapshotFlag toSnapshotFlag(FlagEntity flag) {
        List<SnapshotRule> rules = ruleRepository.findByFlagIdOrderByPriorityAsc(flag.getId())
                .stream()
                .filter(RuleEntity::isEnabled)
                .map(this::toSnapshotRule)
                .toList();
        return new SnapshotFlag(
                flag.getFlagKey(),
                flag.getType(),
                flag.isEnabled(),
                flag.getDefaultValue(),
                flag.getReleaseKey(),
                rules
        );
    }

    private SnapshotRule toSnapshotRule(RuleEntity rule) {
        List<SnapshotCondition> conditions = readConditions(rule.getConditionJson())
                .stream()
                .map(condition -> new SnapshotCondition(condition.attribute(), condition.operator(), condition.value()))
                .toList();
        return new SnapshotRule(
                String.valueOf(rule.getId()),
                rule.getPriority(),
                conditions,
                rule.getRolloutPercentage(),
                rule.getVariationValue()
        );
    }

    private List<ConditionRequest> readConditions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid condition JSON", ex);
        }
    }

    private Snapshot readSnapshot(String json) {
        try {
            return objectMapper.readValue(json, Snapshot.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid snapshot JSON", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize JSON", ex);
        }
    }

    private String checksum(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to calculate checksum", ex);
        }
    }
}
