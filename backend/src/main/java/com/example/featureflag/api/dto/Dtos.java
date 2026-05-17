package com.example.featureflag.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() {
    }

    public record CreateAppRequest(
            String appKey,
            String name,
            String owner
    ) {
    }

    public record AppResponse(Long id, String appKey, String name, String owner) {
    }

    public record CreateFlagRequest(
            String flag,
            String flagKey,
            String description,
            String type,
            String release,
            Boolean enabled
    ) {
    }

    public record UpdateFlagRequest(
            String description,
            String type,
            String status
    ) {
    }

    public record FlagResponse(
            Long id,
            Long configId,
            String flag,
            String flagKey,
            String appKey,
            String environment,
            String description,
            String type,
            boolean enabled,
            String releaseKey,
            String status,
            int rolloutPercentage,
            String conditionJson,
            List<RuleResponse> rules
    ) {
    }

    public record ConfigureFlagRequest(
            List<String> appKeys,
            String environment,
            List<String> regions,
            String subject,
            Boolean enabled,
            Integer rolloutPercentage,
            String conditionJson
    ) {
    }

    public record AddRuleRequest(
            String appKey,
            String environment,
            int priority,
            List<ConditionRequest> conditions,
            String conditionJson,
            int rolloutPercentage,
            Boolean enabled
    ) {
    }

    public record ConditionRequest(
            String attribute,
            String operator,
            String value
    ) {
    }

    public record RuleResponse(
            Long id,
            int priority,
            List<ConditionRequest> conditions,
            String conditionJson,
            int rolloutPercentage,
            boolean enabled
    ) {
    }

    public record PublishRequest(
            String appKey,
            String environment,
            String actor
    ) {
    }

    public record SnapshotResponse(
            String appKey,
            String environment,
            long version,
            String checksum,
            Instant publishedAt
    ) {
    }

    public record EvaluationContext(
            String subjectKey,
            String region,
            String subject,
            String subjectGroup,
            String release,
            String releaseKey,
            Map<String, String> attributes
    ) {
    }

    public record EvaluationRequest(
            String appKey,
            String environment,
            EvaluationContext context
    ) {
    }

    public record BatchEvaluationRequest(
            String appKey,
            String environment,
            List<String> flagKeys,
            EvaluationContext context
    ) {
    }

    public record EvaluationResponse(
            String flagKey,
            boolean enabled,
            String reasonCode,
            String matchedRuleId,
            long snapshotVersion,
            String releaseKey
    ) {
    }

    public record ExplainResponse(
            String flagKey,
            boolean enabled,
            String reasonCode,
            String appKey,
            String environment,
            String subjectKeyHash,
            String matchedRuleId,
            List<String> matchedConditions,
            Integer rolloutBucket,
            String releaseKey,
            long snapshotVersion,
            Instant evaluatedAt
    ) {
    }
}
