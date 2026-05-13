package com.example.featureflagsdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
