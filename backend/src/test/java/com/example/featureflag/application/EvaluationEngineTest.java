package com.example.featureflag.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.featureflag.api.dto.Dtos.EvaluationContext;
import com.example.featureflag.application.EvaluationEngine.EvaluationDecision;
import com.example.featureflag.application.model.SnapshotModels.Snapshot;
import com.example.featureflag.application.model.SnapshotModels.SnapshotFlag;
import com.example.featureflag.application.model.SnapshotModels.SnapshotRule;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class EvaluationEngineTest {
    private final EvaluationEngine engine = new EvaluationEngine();

    @Test
    public void returnsDefaultWhenFlagIsMissing() {
        Snapshot snapshot = new Snapshot("checkout-service", "local", 1, "checksum", List.of());

        EvaluationDecision decision = engine.evaluate(snapshot, "missing-flag", context("u1", "us-east"), "false");

        assertThat(decision.finalValue()).isEqualTo("false");
        assertThat(decision.reasonCode()).isEqualTo("FLAG_NOT_FOUND");
    }

    @Test
    public void returnsFlagDefaultWhenDisabled() {
        Snapshot snapshot = new Snapshot("checkout-service", "local", 1, "checksum", List.of(
                new SnapshotFlag("new-checkout", "boolean", false, "false", "release-1", List.of())
        ));

        EvaluationDecision decision = engine.evaluate(snapshot, "new-checkout", context("u1", "us-east"), "false");

        assertThat(decision.finalValue()).isEqualTo("false");
        assertThat(decision.reasonCode()).isEqualTo("FLAG_DISABLED");
    }

    @Test
    public void returnsVariationWhenRuleMatches() {
        Snapshot snapshot = snapshotWithRule(100);

        EvaluationDecision decision = engine.evaluate(snapshot, "new-checkout", context("u1", "us-east"), "false");

        assertThat(decision.finalValue()).isEqualTo("true");
        assertThat(decision.reasonCode()).isEqualTo("RULE_MATCH");
        assertThat(decision.matchedRuleId()).isEqualTo("rule-1");
        assertThat(decision.snapshotVersion()).isEqualTo(7);
    }

    @Test
    public void returnsDefaultWhenRuleConditionDoesNotMatch() {
        Snapshot snapshot = snapshotWithRule(100);

        EvaluationDecision decision = engine.evaluate(snapshot, "new-checkout", context("u1", "eu-west"), "false");

        assertThat(decision.finalValue()).isEqualTo("false");
        assertThat(decision.reasonCode()).isEqualTo("DEFAULT_VALUE");
    }

    @Test
    public void rolloutBucketIsDeterministic() {
        int first = engine.rolloutBucket("new-checkout", "user-123");
        int second = engine.rolloutBucket("new-checkout", "user-123");

        assertThat(first).isEqualTo(second);
        assertThat(first).isBetween(0, 99);
    }

    private Snapshot snapshotWithRule(int rolloutPercentage) {
        return new Snapshot("checkout-service", "local", 7, "checksum", List.of(
                new SnapshotFlag("new-checkout", "boolean", true, "false", "release-1", List.of(
                new SnapshotRule("rule-1", 1, Map.of("region", "us-east"), rolloutPercentage, "true")
                ))
        ));
    }

    private EvaluationContext context(String subjectKey, String region) {
        return new EvaluationContext(subjectKey, region, null, null, null, null, Map.of("region", region, "platform", "cli"));
    }
}
