package com.example.featureflag.application.model;

import java.util.List;

public final class SnapshotModels {
    private SnapshotModels() {
    }

    public record Snapshot(
            String appKey,
            String environment,
            long version,
            String checksum,
            List<SnapshotFlag> flags
    ) {
    }

    public record SnapshotFlag(
            String flagKey,
            String type,
            boolean enabled,
            String defaultValue,
            String releaseKey,
            List<SnapshotRule> rules
    ) {
    }

    public record SnapshotRule(
            String ruleId,
            int priority,
            List<SnapshotCondition> conditions,
            int rolloutPercentage,
            String variationValue
    ) {
    }

    public record SnapshotCondition(
            String attribute,
            String operator,
            String value
    ) {
    }
}
