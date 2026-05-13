package com.example.featureflagsdk;

public record FeatureEvaluation(
        String flagKey,
        boolean enabled,
        String value,
        String reasonCode,
        String matchedRuleId,
        long snapshotVersion,
        String releaseKey
) {
}
