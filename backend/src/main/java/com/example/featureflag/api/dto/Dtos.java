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
            String flagKey,
            String appKey,
            String environment,
            String name,
            String description,
            String type,
            String defaultValue,
            Boolean enabled,
            String releaseKey
    ) {
    }

    public record UpdateFlagRequest(
            String appKey,
            String environment,
            String name,
            String description,
            String defaultValue,
            Boolean enabled,
            String releaseKey,
            String status
    ) {
    }

    public record FlagResponse(
            Long id,
            String flagKey,
            String appKey,
            String environment,
            String name,
            String description,
            String type,
            String defaultValue,
            boolean enabled,
            String releaseKey,
            String status,
            List<RuleResponse> rules
    ) {
    }

    public record AddRuleRequest(
            String appKey,
            String environment,
            int priority,
            List<ConditionRequest> conditions,
            int rolloutPercentage,
            String variationValue,
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
            int rolloutPercentage,
            String variationValue,
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
            Map<String, String> attributes
    ) {
    }

    public record EvaluationRequest(
            String appKey,
            String environment,
            EvaluationContext context,
            String defaultValue
    ) {
    }

    public record BatchEvaluationRequest(
            String appKey,
            String environment,
            List<String> flagKeys,
            EvaluationContext context,
            String defaultValue
    ) {
    }

    public record EvaluationResponse(
            String flagKey,
            boolean enabled,
            String value,
            String reasonCode,
            String matchedRuleId,
            long snapshotVersion,
            String releaseKey
    ) {
    }

    public record ExplainResponse(
            String flagKey,
            String finalValue,
            String reasonCode,
            String appKey,
            String environment,
            String subjectKeyHash,
            String matchedRuleId,
            List<String> matchedConditions,
            Integer rolloutBucket,
            Integer rolloutPercentage,
            String releaseKey,
            long snapshotVersion,
            Instant evaluatedAt
    ) {
    }
}
