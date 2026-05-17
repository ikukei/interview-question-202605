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
    public void returnsNotFoundWhenFlagIsMissing() {
        Snapshot snapshot = new Snapshot("checkout-service", "local", 1, "checksum", List.of());

        EvaluationDecision decision = engine.evaluate(snapshot, "missing-flag", context("u1", "Asia"));

        assertThat(decision.enabled()).isFalse();
        assertThat(decision.reasonCode()).isEqualTo("FLAG_NOT_FOUND");
    }

    @Test
    public void returnsFlagDisabledWhenFlagKillSwitchIsOff() {
        Snapshot snapshot = new Snapshot("checkout-service", "local", 1, "checksum", List.of(
                new SnapshotFlag("google-sso", "boolean", false, "release-1", List.of())
        ));

        EvaluationDecision decision = engine.evaluate(snapshot, "google-sso", context("u1", "Asia"));

        assertThat(decision.enabled()).isFalse();
        assertThat(decision.reasonCode()).isEqualTo("FLAG_DISABLED");
    }

    @Test
    public void returnsRuleMatchWhenConditionMatches() {
        Snapshot snapshot = snapshotWithRule(100);

        EvaluationDecision decision = engine.evaluate(snapshot, "google-sso", context("u1", "Asia"));

        assertThat(decision.enabled()).isTrue();
        assertThat(decision.reasonCode()).isEqualTo("RULE_MATCH");
        assertThat(decision.matchedRuleId()).isEqualTo("rule-1");
        assertThat(decision.snapshotVersion()).isEqualTo(7);
    }

    @Test
    public void returnsDefaultValueWhenConditionDoesNotMatch() {
        Snapshot snapshot = snapshotWithRule(100);

        // region=Europe does not match the rule condition region=["Asia","North America"]
        EvaluationDecision decision = engine.evaluate(snapshot, "google-sso", context("u1", "Europe"));

        assertThat(decision.enabled()).isFalse();
        assertThat(decision.reasonCode()).isEqualTo("DEFAULT_VALUE");
    }

    @Test
    public void returnsRuleMatchWhenSubjectMatches() {
        SnapshotRule rule = new SnapshotRule("rule-1",
                Map.of("subject", List.of("vip")), 100);
        Snapshot snapshot = new Snapshot("checkout-service", "local", 1, "checksum", List.of(
                new SnapshotFlag("google-sso", "boolean", true, "release-1", List.of(rule))
        ));

        EvaluationDecision decision = engine.evaluate(snapshot, "google-sso",
                new EvaluationContext("u1", "Asia", "vip", null, Map.of()));

        assertThat(decision.enabled()).isTrue();
        assertThat(decision.reasonCode()).isEqualTo("RULE_MATCH");
    }

    @Test
    public void returnsDefaultWhenRolloutExcludesBucket() {
        // rolloutPercentage=0 means no one is included
        Snapshot snapshot = snapshotWithRule(0);

        EvaluationDecision decision = engine.evaluate(snapshot, "google-sso", context("u1", "Asia"));

        assertThat(decision.enabled()).isFalse();
        assertThat(decision.reasonCode()).isEqualTo("ROLLOUT_NOT_INCLUDED");
    }

    @Test
    public void rolloutBucketIsDeterministic() {
        int first = engine.rolloutBucket("google-sso", "user-123");
        int second = engine.rolloutBucket("google-sso", "user-123");

        assertThat(first).isEqualTo(second);
        assertThat(first).isBetween(0, 99);
    }

    @Test
    public void rolloutBucketDiffersAcrossUsers() {
        int b1 = engine.rolloutBucket("google-sso", "user-001");
        int b2 = engine.rolloutBucket("google-sso", "user-002");
        int b3 = engine.rolloutBucket("google-sso", "user-003");
        // Not all three can be the same — with high probability they differ
        assertThat(List.of(b1, b2, b3).stream().distinct().count()).isGreaterThan(1);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Snapshot snapshotWithRule(int rolloutPercentage) {
        SnapshotRule rule = new SnapshotRule("rule-1",
                Map.of("region", List.of("Asia", "North America"), "subject", List.of("vip")),
                rolloutPercentage);
        return new Snapshot("checkout-service", "local", 7, "checksum", List.of(
                new SnapshotFlag("google-sso", "boolean", true, "release-1", List.of(rule))
        ));
    }

    private EvaluationContext context(String subjectKey, String region) {
        return new EvaluationContext(subjectKey, region, "vip", null, Map.of("region", region));
    }
}
