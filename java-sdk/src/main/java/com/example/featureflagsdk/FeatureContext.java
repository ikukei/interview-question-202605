package com.example.featureflagsdk;

import java.util.LinkedHashMap;
import java.util.Map;

public record FeatureContext(
        String subjectKey,
        String region,
        String subject,
        String release,
        Map<String, String> attributes
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String subjectKey;
        private String region;
        private String subject;
        private String release;
        private final Map<String, String> attributes = new LinkedHashMap<>();

        public Builder subjectKey(String subjectKey) {
            this.subjectKey = subjectKey;
            return this;
        }

        public Builder attribute(String key, String value) {
            this.attributes.put(key, value);
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            this.attributes.put("region", region);
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            this.attributes.put("subject", subject);
            return this;
        }

        public Builder release(String release) {
            this.release = release;
            this.attributes.put("release", release);
            return this;
        }

        public FeatureContext build() {
            return new FeatureContext(subjectKey, region, subject, release, Map.copyOf(attributes));
        }
    }
}
