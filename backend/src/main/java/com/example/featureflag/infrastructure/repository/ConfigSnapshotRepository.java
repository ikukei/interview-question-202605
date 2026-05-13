package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.ConfigSnapshotEntity;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ConfigSnapshotRepository {
    private final FeatureDataSource dataSource;

    public ConfigSnapshotRepository(FeatureDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<ConfigSnapshotEntity> findTopByAppKeyAndEnvironmentOrderByVersionDesc(String appKey, String environment) {
        String sql = "select * from ff_config_snapshot where app_key = ? and environment = ? order by version desc limit 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, appKey);
            statement.setString(2, environment);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to find latest snapshot", ex);
        }
    }

    public Optional<ConfigSnapshotEntity> findByAppKeyAndEnvironmentAndVersion(String appKey, String environment, long version) {
        String sql = "select * from ff_config_snapshot where app_key = ? and environment = ? and version = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, appKey);
            statement.setString(2, environment);
            statement.setLong(3, version);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to find snapshot", ex);
        }
    }

    public ConfigSnapshotEntity save(ConfigSnapshotEntity entity) {
        String sql = """
                insert into ff_config_snapshot(app_key, environment, version, checksum, snapshot_json, published_by, published_at)
                values (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, entity.getAppKey());
            statement.setString(2, entity.getEnvironment());
            statement.setLong(3, entity.getVersion());
            statement.setString(4, entity.getChecksum());
            statement.setString(5, entity.getSnapshotJson());
            statement.setString(6, entity.getPublishedBy());
            statement.setTimestamp(7, Timestamp.from(entity.getPublishedAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getLong(1));
                }
            }
            return entity;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to save snapshot", ex);
        }
    }

    private ConfigSnapshotEntity map(ResultSet rs) throws Exception {
        ConfigSnapshotEntity entity = new ConfigSnapshotEntity();
        entity.setId(rs.getLong("id"));
        entity.setAppKey(rs.getString("app_key"));
        entity.setEnvironment(rs.getString("environment"));
        entity.setVersion(rs.getLong("version"));
        entity.setChecksum(rs.getString("checksum"));
        entity.setSnapshotJson(rs.getString("snapshot_json"));
        entity.setPublishedBy(rs.getString("published_by"));
        return entity;
    }
}
