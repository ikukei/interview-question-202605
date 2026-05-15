package com.example.featureflagsdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class FeatureClient {
    private final String baseUrl;
    private final String appKey;
    private final String environment;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private FeatureClient(Builder builder) {
        this.baseUrl = stripTrailingSlash(builder.baseUrl);
        this.appKey = builder.appKey;
        this.environment = builder.environment;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean boolVariation(String flagKey, FeatureContext context, boolean defaultValue) {
        FeatureEvaluation evaluation = evaluate(flagKey, context, Boolean.toString(defaultValue));
        return Boolean.parseBoolean(evaluation.value());
    }

    public FeatureEvaluation evaluate(String flagKey, FeatureContext context, String defaultValue) {
        try {
            Map<String, Object> body = Map.of(
                    "appKey", appKey,
                    "environment", environment,
                    "context", context,
                    "defaultValue", defaultValue
            );
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/evaluations/flags/" + flagKey))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FeatureClientException("Evaluation failed with HTTP " + response.statusCode() + ": " + response.body());
            }
            return objectMapper.readValue(response.body(), FeatureEvaluation.class);
        } catch (Exception ex) {
            throw new FeatureClientException("Unable to evaluate feature flag " + flagKey, ex);
        }
    }

    public List<String> listFlagKeys() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/flags?appKey=" + appKey + "&environment=" + environment))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FeatureClientException("List flags failed with HTTP " + response.statusCode() + ": " + response.body());
            }
            var flags = objectMapper.readTree(response.body());
            return flags.findValuesAsText("flagKey");
        } catch (Exception ex) {
            throw new FeatureClientException("Unable to list feature flags", ex);
        }
    }

    public List<FeatureEvaluation> evaluateAll(FeatureContext context, String defaultValue) {
        try {
            List<String> flagKeys = listFlagKeys();
            if (flagKeys.isEmpty()) {
                return List.of();
            }
            Map<String, Object> body = Map.of(
                    "appKey", appKey,
                    "environment", environment,
                    "flagKeys", flagKeys,
                    "context", Map.of(
                            "subjectKey", context.subjectKey(),
                            "attributes", context.attributes()
                    ),
                    "defaultValue", defaultValue
            );
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/evaluations:batch"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FeatureClientException("Batch evaluation failed with HTTP " + response.statusCode() + ": " + response.body());
            }
            return objectMapper.readValue(response.body(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, FeatureEvaluation.class));
        } catch (Exception ex) {
            throw new FeatureClientException("Unable to evaluate all feature flags", ex);
        }
    }

    private static String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    public static class Builder {
        private String baseUrl = "http://localhost:8080";
        private String appKey = "checkout-service";
        private String environment = "local";

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder appKey(String appKey) {
            this.appKey = appKey;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public FeatureClient build() {
            return new FeatureClient(this);
        }
    }
}
