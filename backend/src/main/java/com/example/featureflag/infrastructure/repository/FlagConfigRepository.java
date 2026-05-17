package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.FlagConfigEntity;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FlagConfigRepository {
    private final JdbcTemplate jdbcTemplate;

    public FlagConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FlagConfigEntity> findByAppKeyAndEnvironmentOrderByIdAsc(String appKey, String environment) {
        String sql = "select * from ff_flag_config where app_key = ? and environment = ? order by id";
        return jdbcTemplate.query(sql, this::mapRow, appKey, environment);
    }

    public Optional<FlagConfigEntity> findByFlagIdAndAppKeyAndEnvironment(Long flagId, String appKey, String environment) {
        String sql = "select * from ff_flag_config where flag_id = ? and app_key = ? and environment = ?";
        List<FlagConfigEntity> result = jdbcTemplate.query(sql, this::mapRow, flagId, appKey, environment);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public FlagConfigEntity save(FlagConfigEntity config) {
        return config.getId() == null ? insert(config) : update(config);
    }

    private FlagConfigEntity insert(FlagConfigEntity config) {
        long nextId = jdbcTemplate.queryForObject("select ff_flag_config_seq.nextval from dual", Long.class);
        String sql = """
                insert into ff_flag_config(id, flag_id, app_key, environment, flag_value, enabled, release_key, rollout_percentage, status, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, nextId, config.getFlagId(), config.getAppKey(), config.getEnvironment(),
                config.getValue(), config.isEnabled() ? 1 : 0, config.getReleaseKey(),
                config.getRolloutPercentage(), config.getStatus(),
                Timestamp.from(config.getCreatedAt()), Timestamp.from(config.getUpdatedAt()));
        config.setId(nextId);
        return config;
    }

    private FlagConfigEntity update(FlagConfigEntity config) {
        String sql = """
                update ff_flag_config set flag_id = ?, app_key = ?, environment = ?, flag_value = ?, enabled = ?,
                  release_key = ?, rollout_percentage = ?, status = ?, updated_at = ? where id = ?
                """;
        jdbcTemplate.update(sql, config.getFlagId(), config.getAppKey(), config.getEnvironment(),
                config.getValue(), config.isEnabled() ? 1 : 0, config.getReleaseKey(),
                config.getRolloutPercentage(), config.getStatus(),
                Timestamp.from(config.getUpdatedAt()), config.getId());
        return config;
    }

    private FlagConfigEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        FlagConfigEntity config = new FlagConfigEntity();
        config.setId(rs.getLong("id"));
        config.setFlagId(rs.getLong("flag_id"));
        config.setAppKey(rs.getString("app_key"));
        config.setEnvironment(rs.getString("environment"));
        config.setValue(rs.getString("flag_value"));
        config.setEnabled(rs.getInt("enabled") == 1);
        config.setReleaseKey(rs.getString("release_key"));
        config.setRolloutPercentage(rs.getInt("rollout_percentage"));
        config.setStatus(rs.getString("status"));
        return config;
    }
}
