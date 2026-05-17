package com.example.featureflag.application;

import com.example.featureflag.api.dto.Dtos.EvaluationContext;
import com.example.featureflag.application.model.SnapshotModels.Snapshot;
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
    public EvaluationDecision evaluate(Snapshot snapshot, String flagKey, EvaluationContext context) {
        EvaluationContext safeContext = context == null ? new EvaluationContext("anonymous", null, null, null, null, null, Map.of()) : context;
        Optional<SnapshotFlag> maybeFlag = snapshot.flags().stream()
                .filter(flag -> flag.flagKey().equals(flagKey))
                .findFirst();

        if (maybeFlag.isEmpty()) {
            return EvaluationDecision.of(flagKey, false, "FLAG_NOT_FOUND", snapshot, safeContext, null, List.of(), null, null);
        }

        SnapshotFlag flag = maybeFlag.get();
        if (!flag.enabled()) {
            return EvaluationDecision.of(flag.flagKey(), false, "FLAG_DISABLED", snapshot, safeContext, null, List.of(), null, flag.releaseKey());
        }

        for (SnapshotRule rule : flag.rules()) {
            List<String> matchedConditions = new ArrayList<>();
            if (matches(rule, safeContext, matchedConditions)) {
                int bucket = rolloutBucket(flag.flagKey(), safeContext.subjectKey());
                if (bucket < rule.rolloutPercentage()) {
                    return EvaluationDecision.of(flag.flagKey(), true, "RULE_MATCH", snapshot, safeContext, rule.ruleId(), matchedConditions, bucket, flag.releaseKey());
                }
                return EvaluationDecision.of(flag.flagKey(), false, "ROLLOUT_NOT_INCLUDED", snapshot, safeContext, rule.ruleId(), matchedConditions, bucket, flag.releaseKey());
            }
        }

        return EvaluationDecision.of(flag.flagKey(), false, "DEFAULT_VALUE", snapshot, safeContext, null, List.of(), null, flag.releaseKey());
    }

    private boolean matches(SnapshotRule rule, EvaluationContext context, List<String> matchedConditions) {
        for (Map.Entry<String, Object> entry : rule.conditionJson().entrySet()) {
            String actual = attributeValue(context, entry.getKey());
            if (!matchesExpected(actual, entry.getValue())) {
                return false;
            }
            matchedConditions.add(entry.getKey() + "=" + entry.getValue());
        }
        return true;
    }

    private boolean matchesExpected(String actual, Object expected) {
        if (expected == null || isBlank(String.valueOf(expected))) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        if (expected instanceof List<?> values) {
            return values.stream().map(String::valueOf).anyMatch(actual::equals);
        }
        return actual.equals(String.valueOf(expected));
    }

    private String attributeValue(EvaluationContext context, String key) {
        return switch (key) {
            case "region" -> firstNonBlank(context.region(), attributes(context).get("region"));
            case "subject", "subjectGroup" -> firstNonBlank(context.subject(), firstNonBlank(context.subjectGroup(), attributes(context).get("subject")));
            case "release", "releaseKey" -> firstNonBlank(context.release(), firstNonBlank(context.releaseKey(), attributes(context).get("release")));
            default -> attributes(context).get(key);
        };
    }

    private Map<String, String> attributes(EvaluationContext context) {
        return context.attributes() == null ? Map.of() : context.attributes();
    }

    public int rolloutBucket(String flagKey, String subjectKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((flagKey + ":" + safeSubject(subjectKey)).getBytes(StandardCharsets.UTF_8));
            int value = ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16) | ((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF);
            return Math.floorMod(value, 100);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute rollout bucket", ex);
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(safeSubject(value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash subject key", ex);
        }
    }

    private static String safeSubject(String value) {
        return value == null || value.isBlank() ? "anonymous" : value;
    }

    private String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : second;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record EvaluationDecision(
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
        static EvaluationDecision of(
                String flagKey,
                boolean enabled,
                String reasonCode,
                Snapshot snapshot,
                EvaluationContext context,
                String matchedRuleId,
                List<String> matchedConditions,
                Integer rolloutBucket,
                String releaseKey
        ) {
            return new EvaluationDecision(
                    flagKey,
                    enabled,
                    reasonCode,
                    snapshot.appKey(),
                    snapshot.environment(),
                    sha256(context.subjectKey()),
                    matchedRuleId,
                    List.copyOf(matchedConditions),
                    rolloutBucket,
                    releaseKey,
                    snapshot.version(),
                    Instant.now()
            );
        }
    }
}
