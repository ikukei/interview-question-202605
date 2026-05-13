package com.example.featureflag.application;

import com.example.featureflag.api.dto.Dtos.AddRuleRequest;
import com.example.featureflag.api.dto.Dtos.AppResponse;
import com.example.featureflag.api.dto.Dtos.ConditionRequest;
import com.example.featureflag.api.dto.Dtos.CreateAppRequest;
import com.example.featureflag.api.dto.Dtos.CreateFlagRequest;
import com.example.featureflag.api.dto.Dtos.FlagResponse;
import com.example.featureflag.api.dto.Dtos.RuleResponse;
import com.example.featureflag.api.dto.Dtos.UpdateFlagRequest;
import com.example.featureflag.domain.ApplicationEntity;
import com.example.featureflag.domain.FlagEntity;
import com.example.featureflag.domain.RuleEntity;
import com.example.featureflag.infrastructure.repository.ApplicationRepository;
import com.example.featureflag.infrastructure.repository.FlagRepository;
import com.example.featureflag.infrastructure.repository.RuleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FlagService {
    private final ApplicationRepository applicationRepository;
    private final FlagRepository flagRepository;
    private final RuleRepository ruleRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public FlagService(
            ApplicationRepository applicationRepository,
            FlagRepository flagRepository,
            RuleRepository ruleRepository,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.applicationRepository = applicationRepository;
        this.flagRepository = flagRepository;
        this.ruleRepository = ruleRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public AppResponse createApp(CreateAppRequest request) {
        applicationRepository.findByAppKey(request.appKey()).ifPresent(existing -> {
            throw new IllegalArgumentException("Application already exists: " + request.appKey());
        });
        ApplicationEntity app = new ApplicationEntity();
        app.setAppKey(request.appKey());
        app.setName(request.name());
        app.setOwner(request.owner());
        ApplicationEntity saved = applicationRepository.save(app);
        auditService.record("demo-user", "create", "application", request.appKey(), null, request.toString());
        return new AppResponse(saved.getId(), saved.getAppKey(), saved.getName(), saved.getOwner());
    }

    public List<AppResponse> listApps() {
        return applicationRepository.findAll().stream()
                .map(app -> new AppResponse(app.getId(), app.getAppKey(), app.getName(), app.getOwner()))
                .toList();
    }

    public FlagResponse createFlag(CreateFlagRequest request) {
        flagRepository.findByFlagKeyAndAppKeyAndEnvironment(request.flagKey(), request.appKey(), request.environment())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Flag already exists in scope: " + request.flagKey());
                });
        FlagEntity flag = new FlagEntity();
        flag.setFlagKey(request.flagKey());
        flag.setAppKey(request.appKey());
        flag.setEnvironment(request.environment());
        flag.setName(request.name());
        flag.setDescription(request.description());
        flag.setType(request.type() == null || request.type().isBlank() ? "boolean" : request.type());
        flag.setDefaultValue(request.defaultValue());
        flag.setEnabled(request.enabled() == null || request.enabled());
        flag.setReleaseKey(request.releaseKey());
        FlagEntity saved = flagRepository.save(flag);
        auditService.record("demo-user", "create", "flag", scopedKey(saved), null, request.toString());
        return toFlagResponse(saved);
    }

    public List<FlagResponse> listFlags(String appKey, String environment) {
        return flagRepository.findByAppKeyAndEnvironmentOrderByFlagKeyAsc(appKey, environment)
                .stream()
                .map(this::toFlagResponse)
                .toList();
    }

    public FlagResponse updateFlag(String flagKey, String appKey, String environment, UpdateFlagRequest request) {
        FlagEntity flag = findFlag(flagKey, appKey, environment);
        if (request.name() != null) {
            flag.setName(request.name());
        }
        if (request.description() != null) {
            flag.setDescription(request.description());
        }
        if (request.defaultValue() != null) {
            flag.setDefaultValue(request.defaultValue());
        }
        if (request.enabled() != null) {
            flag.setEnabled(request.enabled());
        }
        if (request.releaseKey() != null) {
            flag.setReleaseKey(request.releaseKey());
        }
        if (request.status() != null) {
            flag.setStatus(request.status());
        }
        flag.touch();
        auditService.record("demo-user", "update", "flag", scopedKey(flag), null, request.toString());
        return toFlagResponse(flagRepository.save(flag));
    }

    public RuleResponse addRule(String flagKey, AddRuleRequest request) {
        FlagEntity flag = findFlag(flagKey, request.appKey(), request.environment());
        RuleEntity rule = new RuleEntity();
        rule.setFlagId(flag.getId());
        rule.setPriority(request.priority());
        rule.setConditionJson(writeJson(request.conditions()));
        rule.setRolloutPercentage(request.rolloutPercentage());
        rule.setVariationValue(request.variationValue());
        rule.setEnabled(request.enabled() == null || request.enabled());
        RuleEntity saved = ruleRepository.save(rule);
        auditService.record("demo-user", "create", "rule", scopedKey(flag) + ":" + saved.getId(), null, request.toString());
        return toRuleResponse(saved);
    }

    public FlagResponse archiveFlag(String flagKey, String appKey, String environment) {
        FlagEntity flag = findFlag(flagKey, appKey, environment);
        flag.setStatus("archived");
        flag.setEnabled(false);
        flag.touch();
        auditService.record("demo-user", "archive", "flag", scopedKey(flag), null, null);
        return toFlagResponse(flagRepository.save(flag));
    }

    private FlagEntity findFlag(String flagKey, String appKey, String environment) {
        return flagRepository.findByFlagKeyAndAppKeyAndEnvironment(flagKey, appKey, environment)
                .orElseThrow(() -> new NotFoundException("Flag not found: " + flagKey));
    }

    private FlagResponse toFlagResponse(FlagEntity flag) {
        List<RuleResponse> rules = ruleRepository.findByFlagIdOrderByPriorityAsc(flag.getId())
                .stream()
                .map(this::toRuleResponse)
                .toList();
        return new FlagResponse(
                flag.getId(),
                flag.getFlagKey(),
                flag.getAppKey(),
                flag.getEnvironment(),
                flag.getName(),
                flag.getDescription(),
                flag.getType(),
                flag.getDefaultValue(),
                flag.isEnabled(),
                flag.getReleaseKey(),
                flag.getStatus(),
                rules
        );
    }

    private RuleResponse toRuleResponse(RuleEntity rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getPriority(),
                readConditions(rule.getConditionJson()),
                rule.getRolloutPercentage(),
                rule.getVariationValue(),
                rule.isEnabled()
        );
    }

    private List<ConditionRequest> readConditions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid rule condition JSON", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize JSON", ex);
        }
    }

    private String scopedKey(FlagEntity flag) {
        return flag.getEnvironment() + ":" + flag.getAppKey() + ":" + flag.getFlagKey();
    }
}
