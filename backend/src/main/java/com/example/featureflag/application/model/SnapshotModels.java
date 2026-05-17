package com.example.featureflag.application.model;

import java.util.List;
import java.util.Map;

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
            String releaseKey,
            List<SnapshotRule> rules
    ) {
    }

    public record SnapshotRule(
            String ruleId,
            int priority,
            Map<String, Object> conditionJson,
            int rolloutPercentage
    ) {
    }
}
