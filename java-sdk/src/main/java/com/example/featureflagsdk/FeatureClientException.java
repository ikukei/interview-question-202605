package com.example.featureflagsdk;

public class FeatureClientException extends RuntimeException {
    public FeatureClientException(String message) {
        super(message);
    }

    public FeatureClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
