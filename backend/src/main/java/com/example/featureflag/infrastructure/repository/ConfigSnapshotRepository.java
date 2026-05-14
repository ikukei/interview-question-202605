package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.ConfigSnapshotEntity;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ConfigSnapshotRepository {
    private final JdbcTemplate jdbcTemplate;

    public ConfigSnapshotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ConfigSnapshotEntity> findTopByAppKeyAndEnvironmentOrderByVersionDesc(String appKey, String environment) {
        String sql = "select * from ff_config_snapshot where app_key = ? and environment = ? order by version desc limit 1";
        List<ConfigSnapshotEntity> result = jdbcTemplate.query(sql, this::mapRow, appKey, environment);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public Optional<ConfigSnapshotEntity> findByAppKeyAndEnvironmentAndVersion(String appKey, String environment, long version) {
        String sql = "select * from ff_config_snapshot where app_key = ? and environment = ? and version = ?";
        List<ConfigSnapshotEntity> result = jdbcTemplate.query(sql, this::mapRow, appKey, environment, version);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public ConfigSnapshotEntity save(ConfigSnapshotEntity entity) {
        String sql = """
                insert into ff_config_snapshot(app_key, environment, version, checksum, snapshot_json, published_by, published_at)
                values (?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, entity.getAppKey());
            stmt.setString(2, entity.getEnvironment());
            stmt.setLong(3, entity.getVersion());
            stmt.setString(4, entity.getChecksum());
            stmt.setString(5, entity.getSnapshotJson());
            stmt.setString(6, entity.getPublishedBy());
            stmt.setTimestamp(7, Timestamp.from(entity.getPublishedAt()));
            return stmt;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            entity.setId(keyHolder.getKey().longValue());
        }
        return entity;
    }

    private ConfigSnapshotEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
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
