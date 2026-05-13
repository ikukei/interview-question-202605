package com.example.featureflag.application;

import com.example.featureflag.application.model.SnapshotModels.Snapshot;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SnapshotCache {
    private final Map<String, Snapshot> latestByScope = new ConcurrentHashMap<>();

    public Optional<Snapshot> get(String appKey, String environment) {
        return Optional.ofNullable(latestByScope.get(cacheKey(appKey, environment)));
    }

    public void put(Snapshot snapshot) {
        latestByScope.put(cacheKey(snapshot.appKey(), snapshot.environment()), snapshot);
    }

    private String cacheKey(String appKey, String environment) {
        return environment + ":" + appKey;
    }
}
