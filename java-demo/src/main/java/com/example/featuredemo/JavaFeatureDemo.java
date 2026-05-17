package com.example.featuredemo;

import com.example.featureflagsdk.FeatureClient;
import com.example.featureflagsdk.FeatureContext;
import com.example.featureflagsdk.FeatureEvaluation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JavaFeatureDemo {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static void main(String[] args) {
        String baseUrl = arg(args, 0, "http://localhost:8080");
        String subjectKey = arg(args, 1, "java-demo-user");
        String region = arg(args, 2, "Asia");
        String subject = arg(args, 3, "vip");
        String release = arg(args, 4, LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        int pollInterval = Integer.parseInt(arg(args, 5, "3"));

        FeatureClient client = FeatureClient.builder()
                .baseUrl(baseUrl)
                .appKey("java-demo")
                .environment("local")
                .build();

        FeatureContext context = FeatureContext.builder()
                .subjectKey(subjectKey)
                .region(region)
                .subject(subject)
                .release(release)
                .attribute("platform", "java-cli")
                .build();

        Set<String> knownFlags = new HashSet<>();
        
        System.out.println("=== Java Feature Flag Demo ===");
        System.out.println("App: java-demo, region: " + region + ", subject: " + subject + ", release: " + release);
        System.out.println("Polling for feature flags every " + pollInterval + " seconds...");
        System.out.println("Press Ctrl+C to exit.\n");

        while (true) {
            try {
                List<String> currentFlags = client.listFlagKeys();
                
                for (String flagKey : currentFlags) {
                    if (!knownFlags.contains(flagKey)) {
                        System.out.println("\n[" + LocalDateTime.now().format(FORMATTER) + "] NEW FLAG DETECTED: " + flagKey);
                        try {
                            FeatureEvaluation evaluation = client.evaluate(flagKey, context, "false");
                            printEvaluation(evaluation);
                        } catch (Exception e) {
                            System.err.println("  Evaluation failed: " + e.getMessage());
                        } finally {
                            knownFlags.add(flagKey);
                        }
                    }
                }

                if (!currentFlags.isEmpty()) {
                    System.out.println("\n[" + LocalDateTime.now().format(FORMATTER) + "] All feature flags (" + currentFlags.size() + "):");
                    List<FeatureEvaluation> evaluations = client.evaluateAll(context, "false");
                    for (FeatureEvaluation eval : evaluations) {
                        System.out.println("- " + eval.flagKey() + ": " + eval.value() + " (enabled: " + eval.enabled() + ")");
                    }
                } else {
                    System.out.println("[" + LocalDateTime.now().format(FORMATTER) + "] No feature flags found");
                }

                Thread.sleep(pollInterval * 1000);
            } catch (Exception e) {
                System.err.println("[" + LocalDateTime.now().format(FORMATTER) + "] Error: " + e.getMessage());
                try {
                    Thread.sleep(pollInterval * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private static void printEvaluation(FeatureEvaluation evaluation) {
        System.out.println("  Flag Key: " + evaluation.flagKey());
        System.out.println("  Value: " + evaluation.value());
        System.out.println("  Enabled: " + evaluation.enabled());
        System.out.println("  Reason: " + evaluation.reasonCode());
        System.out.println("  Snapshot Version: " + evaluation.snapshotVersion());
        System.out.println("  Release: " + evaluation.releaseKey());
    }

    private static String arg(String[] args, int index, String defaultValue) {
        return args.length > index && !args[index].isBlank() ? args[index] : defaultValue;
    }
}
