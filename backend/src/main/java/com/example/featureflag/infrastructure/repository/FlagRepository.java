package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.FlagEntity;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FlagRepository {
    private final JdbcTemplate jdbcTemplate;

    public FlagRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FlagEntity> findByAppKeyAndEnvironmentOrderByFlagKeyAsc(String appKey, String environment) {
        String sql = "select * from ff_flag where app_key = ? and environment = ? order by flag_key";
        return jdbcTemplate.query(sql, this::mapRow, appKey, environment);
    }

    public Optional<FlagEntity> findByFlagKeyAndAppKeyAndEnvironment(String flagKey, String appKey, String environment) {
        String sql = "select * from ff_flag where flag_key = ? and app_key = ? and environment = ?";
        List<FlagEntity> result = jdbcTemplate.query(sql, this::mapRow, flagKey, appKey, environment);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public FlagEntity save(FlagEntity flag) {
        return flag.getId() == null ? insert(flag) : update(flag);
    }

    private FlagEntity insert(FlagEntity flag) {
        long nextId = jdbcTemplate.queryForObject("select ff_flag_seq.nextval from dual", Long.class);
        String sql = """
                insert into ff_flag(id, flag_key, app_key, environment, name, description, type, default_value, enabled, release_key, status, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, nextId, flag.getFlagKey(), flag.getAppKey(), flag.getEnvironment(),
                flag.getName(), flag.getDescription(), flag.getType(), flag.getDefaultValue(),
                flag.isEnabled() ? 1 : 0, flag.getReleaseKey(), flag.getStatus(),
                Timestamp.from(flag.getCreatedAt()), Timestamp.from(flag.getUpdatedAt()));
        flag.setId(nextId);
        return flag;
    }

    private FlagEntity update(FlagEntity flag) {
        String sql = """
                update ff_flag set flag_key = ?, app_key = ?, environment = ?, name = ?, description = ?, type = ?, default_value = ?,
                  enabled = ?, release_key = ?, status = ?, created_at = ?, updated_at = ? where id = ?
                """;
        jdbcTemplate.update(sql, flag.getFlagKey(), flag.getAppKey(), flag.getEnvironment(), flag.getName(),
                flag.getDescription(), flag.getType(), flag.getDefaultValue(), flag.isEnabled() ? 1 : 0,
                flag.getReleaseKey(), flag.getStatus(), Timestamp.from(flag.getCreatedAt()),
                Timestamp.from(flag.getUpdatedAt()), flag.getId());
        return flag;
    }

    private FlagEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        FlagEntity flag = new FlagEntity();
        flag.setId(rs.getLong("id"));
        flag.setFlagKey(rs.getString("flag_key"));
        flag.setAppKey(rs.getString("app_key"));
        flag.setEnvironment(rs.getString("environment"));
        flag.setName(rs.getString("name"));
        flag.setDescription(rs.getString("description"));
        flag.setType(rs.getString("type"));
        flag.setDefaultValue(rs.getString("default_value"));
        flag.setEnabled(rs.getInt("enabled") == 1);
        flag.setReleaseKey(rs.getString("release_key"));
        flag.setStatus(rs.getString("status"));
        return flag;
    }
}