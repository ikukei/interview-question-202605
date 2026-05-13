package com.example.featuredemo;

import com.example.featureflagsdk.FeatureClient;
import com.example.featureflagsdk.FeatureContext;
import com.example.featureflagsdk.FeatureEvaluation;

public class JavaFeatureDemo {
    public static void main(String[] args) {
        String baseUrl = arg(args, 0, "http://localhost:8080");
        String subjectKey = arg(args, 1, "java-demo-user");
        String region = arg(args, 2, "us-east");

        FeatureClient client = FeatureClient.builder()
                .baseUrl(baseUrl)
                .appKey("checkout-service")
                .environment("local")
                .build();

        FeatureContext context = FeatureContext.builder()
                .subjectKey(subjectKey)
                .attribute("region", region)
                .attribute("platform", "java-cli")
                .build();

        FeatureEvaluation evaluation = client.evaluate("new-checkout", context, "false");
        System.out.println("Feature flag: " + evaluation.flagKey());
        System.out.println("Feature value: " + evaluation.value());
        System.out.println("Enabled: " + evaluation.enabled());
        System.out.println("Reason: " + evaluation.reasonCode());
        System.out.println("Snapshot version: " + evaluation.snapshotVersion());
        System.out.println("Release: " + evaluation.releaseKey());
    }

    private static String arg(String[] args, int index, String defaultValue) {
        return args.length > index && !args[index].isBlank() ? args[index] : defaultValue;
    }
}
