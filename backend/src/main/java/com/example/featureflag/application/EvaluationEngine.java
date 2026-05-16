package com.example.featureflag.application;

import com.example.featureflag.api.dto.Dtos.EvaluationContext;
import com.example.featureflag.application.model.SnapshotModels.Snapshot;
import com.example.featureflag.application.model.SnapshotModels.SnapshotCondition;
import com.example.featureflag.application.model.SnapshotModels.SnapshotFlag;
import com.example.featureflag.application.model.SnapshotModels.SnapshotRule;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class EvaluationEngine {
    public EvaluationDecision evaluate(Snapshot snapshot, String flagKey, EvaluationContext context, String callerDefaultValue) {
        String fallback = callerDefaultValue == null ? "false" : callerDefaultValue;
        Optional<SnapshotFlag> maybeFlag = snapshot.flags().stream()
                .filter(flag -> flag.flagKey().equals(flagKey))
                .findFirst();

        if (maybeFlag.isEmpty()) {
            return EvaluationDecision.defaulted(flagKey, fallback, "FLAG_NOT_FOUND", snapshot, context);
        }

        SnapshotFlag flag = maybeFlag.get();
        if (!flag.enabled()) {
            return EvaluationDecision.forFlag(flag, flag.defaultValue(), "FLAG_DISABLED", snapshot, context);
        }

        for (SnapshotRule rule : flag.rules()) {
            List<String> matchedConditions = new ArrayList<>();
            if (matches(rule, context, matchedConditions)) {
                int bucket = rolloutBucket(flag.flagKey(), context.subjectKey());
                if (bucket < rule.rolloutPercentage()) {
                    return EvaluationDecision.matched(flag, rule, rule.variationValue(), snapshot, context, matchedConditions, bucket);
                }
                return EvaluationDecision.ruleSkipped(flag, rule, flag.defaultValue(), snapshot, context, matchedConditions, bucket);
            }
        }

        return EvaluationDecision.forFlag(flag, flag.defaultValue(), "DEFAULT_VALUE", snapshot, context);
    }

    private boolean matches(SnapshotRule rule, EvaluationContext context, List<String> matchedConditions) {
        for (SnapshotCondition condition : rule.conditions()) {
            String actual = attributes(context).get(condition.attribute());
            if (!matchCondition(actual, condition.operator(), condition.value())) {
                return false;
            }
            matchedConditions.add(condition.attribute() + " " + condition.operator() + " " + condition.value());
        }
        return true;
    }

    private boolean matchCondition(String actual, String operator, String expected) {
        if (actual == null) {
            return false;
        }
        return switch (operator) {
            case "equals" -> actual.equals(expected);
            case "notEquals" -> !actual.equals(expected);
            case "contains" -> actual.contains(expected);
            case "in" -> List.of(expected.split(",")).stream().map(String::trim).anyMatch(actual::equals);
            default -> false;
        };
    }

    private Map<String, String> attributes(EvaluationContext context) {
        return context.attributes() == null ? Map.of() : context.attributes();
    }

    public int rolloutBucket(String flagKey, String subjectKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((flagKey + ":" + subjectKey).getBytes(StandardCharsets.UTF_8));
            int value = ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16) | ((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF);
            return Math.floorMod(value, 100);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute rollout bucket", ex);
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash subject key", ex);
        }
    }

    public record EvaluationDecision(
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
        static EvaluationDecision defaulted(String flagKey, String value, String reasonCode, Snapshot snapshot, EvaluationContext context) {
            return new EvaluationDecision(
                    flagKey,
                    value,
                    reasonCode,
                    snapshot.appKey(),
                    snapshot.environment(),
                    sha256(context.subjectKey()),
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    snapshot.version(),
                    Instant.now()
            );
        }

        static EvaluationDecision forFlag(SnapshotFlag flag, String value, String reasonCode, Snapshot snapshot, EvaluationContext context) {
            return new EvaluationDecision(
                    flag.flagKey(),
                    value,
                    reasonCode,
                    snapshot.appKey(),
                    snapshot.environment(),
                    sha256(context.subjectKey()),
                    null,
                    List.of(),
                    null,
                    null,
                    flag.releaseKey(),
                    snapshot.version(),
                    Instant.now()
            );
        }

        static EvaluationDecision matched(
                SnapshotFlag flag,
                SnapshotRule rule,
                String value,
                Snapshot snapshot,
                EvaluationContext context,
                List<String> matchedConditions,
                int bucket
        ) {
            return new EvaluationDecision(
                    flag.flagKey(),
                    value,
                    "RULE_MATCH",
                    snapshot.appKey(),
                    snapshot.environment(),
                    sha256(context.subjectKey()),
                    rule.ruleId(),
                    List.copyOf(matchedConditions),
                    bucket,
                    rule.rolloutPercentage(),
                    flag.releaseKey(),
                    snapshot.version(),
                    Instant.now()
            );
        }

        static EvaluationDecision ruleSkipped(
                SnapshotFlag flag,
                SnapshotRule rule,
                String value,
                Snapshot snapshot,
                EvaluationContext context,
                List<String> matchedConditions,
                int bucket
        ) {
            return new EvaluationDecision(
                    flag.flagKey(),
                    value,
                    "ROLLOUT_NOT_INCLUDED",
                    snapshot.appKey(),
                    snapshot.environment(),
                    sha256(context.subjectKey()),
                    rule.ruleId(),
                    List.copyOf(matchedConditions),
                    bucket,
                    rule.rolloutPercentage(),
                    flag.releaseKey(),
                    snapshot.version(),
                    Instant.now()
            );
        }
    }
}
