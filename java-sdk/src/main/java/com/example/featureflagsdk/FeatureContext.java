package com.example.featureflagsdk;

import java.util.LinkedHashMap;
import java.util.Map;

public record FeatureContext(String subjectKey, Map<String, String> attributes) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String subjectKey;
        private final Map<String, String> attributes = new LinkedHashMap<>();

        public Builder subjectKey(String subjectKey) {
            this.subjectKey = subjectKey;
            return this;
        }

        public Builder attribute(String key, String value) {
            this.attributes.put(key, value);
            return this;
        }

        public FeatureContext build() {
            return new FeatureContext(subjectKey, Map.copyOf(attributes));
        }
    }
}
