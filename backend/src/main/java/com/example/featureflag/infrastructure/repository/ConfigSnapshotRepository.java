package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.ConfigSnapshotEntity;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConfigSnapshotRepository {
    private final JdbcTemplate jdbcTemplate;

    public ConfigSnapshotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ConfigSnapshotEntity> findTopByAppKeyAndEnvironmentOrderByVersionDesc(String appKey, String environment) {
        String sql = "select * from ff_config_snapshot where app_key = ? and environment = ? order by version desc fetch first 1 row only";
        List<ConfigSnapshotEntity> result = jdbcTemplate.query(sql, this::mapRow, appKey, environment);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public Optional<ConfigSnapshotEntity> findByAppKeyAndEnvironmentAndVersion(String appKey, String environment, long version) {
        String sql = "select * from ff_config_snapshot where app_key = ? and environment = ? and version = ?";
        List<ConfigSnapshotEntity> result = jdbcTemplate.query(sql, this::mapRow, appKey, environment, version);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public ConfigSnapshotEntity save(ConfigSnapshotEntity entity) {
        long nextId = jdbcTemplate.queryForObject("select ff_config_snapshot_seq.nextval from dual", Long.class);
        String sql = """
                insert into ff_config_snapshot(id, app_key, environment, version, checksum, snapshot_json, published_by, published_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, nextId, entity.getAppKey(), entity.getEnvironment(),
                entity.getVersion(), entity.getChecksum(), entity.getSnapshotJson(),
                entity.getPublishedBy(), Timestamp.from(entity.getPublishedAt()));
        entity.setId(nextId);
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