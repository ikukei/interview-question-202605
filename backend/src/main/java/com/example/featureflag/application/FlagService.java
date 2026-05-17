package com.example.featureflag.application;

import com.example.featureflag.api.dto.Dtos.AddRuleRequest;
import com.example.featureflag.api.dto.Dtos.AppResponse;
import com.example.featureflag.api.dto.Dtos.ConditionRequest;
import com.example.featureflag.api.dto.Dtos.ConfigureFlagRequest;
import com.example.featureflag.api.dto.Dtos.CreateAppRequest;
import com.example.featureflag.api.dto.Dtos.CreateFlagRequest;
import com.example.featureflag.api.dto.Dtos.FlagResponse;
import com.example.featureflag.api.dto.Dtos.RuleResponse;
import com.example.featureflag.api.dto.Dtos.UpdateFlagRequest;
import com.example.featureflag.domain.ApplicationEntity;
import com.example.featureflag.domain.FlagConfigEntity;
import com.example.featureflag.domain.FlagEntity;
import com.example.featureflag.domain.RuleEntity;
import com.example.featureflag.infrastructure.repository.ApplicationRepository;
import com.example.featureflag.infrastructure.repository.FlagConfigRepository;
import com.example.featureflag.infrastructure.repository.FlagRepository;
import com.example.featureflag.infrastructure.repository.RuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FlagService {
    private final ApplicationRepository applicationRepository;
    private final FlagRepository flagRepository;
    private final FlagConfigRepository configRepository;
    private final RuleRepository ruleRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public FlagService(
            ApplicationRepository applicationRepository,
            FlagRepository flagRepository,
            FlagConfigRepository configRepository,
            RuleRepository ruleRepository,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.applicationRepository = applicationRepository;
        this.flagRepository = flagRepository;
        this.configRepository = configRepository;
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
        String flagKey = normalizeFlagKey(request.flag(), request.flagKey());
        flagRepository.findByFlagKey(flagKey).ifPresent(existing -> {
            throw new IllegalArgumentException("Flag already exists: " + flagKey);
        });

        FlagEntity flag = new FlagEntity();
        flag.setFlagKey(flagKey);
        flag.setName(flagKey);
        flag.setDescription(blankToDefault(request.description(), "No description"));
        flag.setType(blankToDefault(request.type(), "boolean"));
        flag.setReleaseKey(request.release());
        flag.setEnabled(request.enabled() == null || request.enabled());
        FlagEntity saved = flagRepository.save(flag);
        auditService.record("demo-user", "create", "flag", saved.getFlagKey(), null, request.toString());
        return toFlagResponse(saved, null);
    }

    public List<FlagResponse> listFlags(String appKey, String environment) {
        if (isBlank(appKey) || isBlank(environment)) {
            return flagRepository.findAllOrderByFlagKeyAsc().stream()
                    .map(flag -> toFlagResponse(flag, null))
                    .toList();
        }
        return configRepository.findByAppKeyAndEnvironmentOrderByIdAsc(appKey, environment).stream()
                .map(config -> toFlagResponse(findFlagById(config.getFlagId()), config))
                .toList();
    }

    public FlagResponse updateFlag(String flagKey, UpdateFlagRequest request) {
        FlagEntity flag = findFlag(flagKey);
        if (request.description() != null) {
            flag.setDescription(request.description());
        }
        if (request.type() != null) {
            flag.setType(request.type());
        }
        if (request.status() != null) {
            flag.setStatus(request.status());
        }
        flag.touch();
        auditService.record("demo-user", "update", "flag", flag.getFlagKey(), null, request.toString());
        return toFlagResponse(flagRepository.save(flag), null);
    }

    public List<FlagResponse> configureFlag(String flagKey, ConfigureFlagRequest request) {
        FlagEntity flag = findFlag(flagKey);
        List<String> appKeys = request.appKeys() == null || request.appKeys().isEmpty()
                ? List.of("vue-demo")
                : request.appKeys();

        return appKeys.stream()
                .map(appKey -> configureOneApp(flag, appKey, request))
                .toList();
    }

    public RuleResponse addRule(String flagKey, AddRuleRequest request) {
        FlagEntity flag = findFlag(flagKey);
        FlagConfigEntity config = configRepository
                .findByFlagIdAndAppKeyAndEnvironment(flag.getId(), request.appKey(), request.environment())
                .orElseThrow(() -> new NotFoundException("Flag config not found for " + flagKey));

        RuleEntity rule = new RuleEntity();
        rule.setFlagId(flag.getId());
        rule.setConfigId(config.getId());
        rule.setPriority(request.priority());
        rule.setConditionJson(firstNonBlank(request.conditionJson(), writeJson(request.conditions() == null ? List.of() : request.conditions())));
        rule.setRolloutPercentage(request.rolloutPercentage());
        rule.setEnabled(request.enabled() == null || request.enabled());
        RuleEntity saved = ruleRepository.save(rule);
        auditService.record("demo-user", "create", "rule", scopedKey(config, flag), null, request.toString());
        return toRuleResponse(saved);
    }

    public FlagResponse archiveFlag(String flagKey, String appKey, String environment) {
        FlagEntity flag = findFlag(flagKey);
        FlagConfigEntity config = configRepository.findByFlagIdAndAppKeyAndEnvironment(flag.getId(), appKey, environment)
                .orElseThrow(() -> new NotFoundException("Flag config not found: " + flagKey));
        config.setStatus("archived");
        config.setEnabled(false);
        config.touch();
        auditService.record("demo-user", "archive", "flag-config", scopedKey(config, flag), null, null);
        return toFlagResponse(flag, configRepository.save(config));
    }

    private FlagResponse configureOneApp(FlagEntity flag, String appKey, ConfigureFlagRequest request) {
        ensureApp(appKey);
        FlagConfigEntity config = configRepository
                .findByFlagIdAndAppKeyAndEnvironment(flag.getId(), appKey, request.environment())
                .orElseGet(() -> {
                    FlagConfigEntity created = new FlagConfigEntity();
                    created.setFlagId(flag.getId());
                    created.setAppKey(appKey);
                    created.setEnvironment(blankToDefault(request.environment(), "local"));
                    return created;
                });

        config.setEnabled(request.enabled() == null || request.enabled());
        config.setReleaseKey(flag.getReleaseKey());
        config.setRolloutPercentage(clampRollout(request.rolloutPercentage()));
        config.setStatus("active");
        config.touch();
        FlagConfigEntity savedConfig = configRepository.save(config);

        RuleEntity rule = new RuleEntity();
        rule.setFlagId(flag.getId());
        rule.setConfigId(savedConfig.getId());
        rule.setPriority(1);
        rule.setConditionJson(buildConditionJson(request));
        rule.setRolloutPercentage(savedConfig.getRolloutPercentage());
        rule.setEnabled(true);
        ruleRepository.saveConfigRule(rule);

        auditService.record("demo-user", "configure", "flag-config", scopedKey(savedConfig, flag), null, request.toString());
        return toFlagResponse(flag, savedConfig);
    }

    private void ensureApp(String appKey) {
        if (applicationRepository.findByAppKey(appKey).isPresent()) {
            return;
        }
        ApplicationEntity app = new ApplicationEntity();
        app.setAppKey(appKey);
        app.setName(appKey);
        app.setOwner("demo");
        applicationRepository.save(app);
    }

    private FlagEntity findFlag(String flagKey) {
        return flagRepository.findByFlagKey(flagKey)
                .orElseThrow(() -> new NotFoundException("Flag not found: " + flagKey));
    }

    private FlagEntity findFlagById(Long id) {
        return flagRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Flag not found: " + id));
    }

    private FlagResponse toFlagResponse(FlagEntity flag, FlagConfigEntity config) {
        List<RuleResponse> rules = config == null
                ? List.of()
                : ruleRepository.findByConfigIdOrderByPriorityAsc(config.getId()).stream()
                        .map(this::toRuleResponse)
                        .toList();
        String conditionJson = rules.isEmpty() ? "{}" : rules.get(0).conditionJson();
        return new FlagResponse(
                flag.getId(),
                config == null ? null : config.getId(),
                flag.getFlagKey(),
                flag.getFlagKey(),
                config == null ? null : config.getAppKey(),
                config == null ? null : config.getEnvironment(),
                flag.getDescription(),
                flag.getType(),
                config == null || config.isEnabled(),
                config == null ? null : config.getReleaseKey(),
                config == null ? flag.getStatus() : config.getStatus(),
                config == null ? 100 : config.getRolloutPercentage(),
                conditionJson,
                rules
        );
    }

    private RuleResponse toRuleResponse(RuleEntity rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getPriority(),
                List.of(),
                rule.getConditionJson(),
                rule.getRolloutPercentage(),
                rule.isEnabled()
        );
    }

    private String buildConditionJson(ConfigureFlagRequest request) {
        if (!isBlank(request.conditionJson())) {
            return request.conditionJson();
        }
        Map<String, Object> condition = new LinkedHashMap<>();
        if (request.regions() != null && !request.regions().isEmpty()) {
            condition.put("region", request.regions());
        }
        if (!isBlank(request.subject())) {
            condition.put("subject", request.subject());
        }
        return writeJson(condition);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize JSON", ex);
        }
    }

    private int clampRollout(Integer rollout) {
        int value = rollout == null ? 100 : rollout;
        return Math.max(0, Math.min(100, value));
    }

    private String normalizeFlagKey(String flag, String flagKey) {
        String value = firstNonBlank(flag, flagKey);
        if (isBlank(value)) {
            throw new IllegalArgumentException("flag is required");
        }
        return value.trim();
    }

    private String scopedKey(FlagConfigEntity config, FlagEntity flag) {
        return config.getEnvironment() + ":" + config.getAppKey() + ":" + flag.getFlagKey();
    }

    private String blankToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first.trim() : second;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
