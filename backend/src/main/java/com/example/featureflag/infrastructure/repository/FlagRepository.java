package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.FlagEntity;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
        String sql = """
                insert into ff_flag(flag_key, app_key, environment, name, description, type, default_value, enabled, release_key, status, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var stmt = connection.prepareStatement(sql, new String[]{"id"});
            bindFlag(stmt, flag);
            return stmt;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            flag.setId(keyHolder.getKey().longValue());
        }
        return flag;
    }

    private FlagEntity update(FlagEntity flag) {
        String sql = """
                update ff_flag set flag_key = ?, app_key = ?, environment = ?, name = ?, description = ?, type = ?, default_value = ?,
                  enabled = ?, release_key = ?, status = ?, created_at = ?, updated_at = ? where id = ?
                """;
        jdbcTemplate.update(sql, flag.getFlagKey(), flag.getAppKey(), flag.getEnvironment(), flag.getName(),
                flag.getDescription(), flag.getType(), flag.getDefaultValue(), flag.isEnabled(),
                flag.getReleaseKey(), flag.getStatus(), Timestamp.from(flag.getCreatedAt()),
                Timestamp.from(flag.getUpdatedAt()), flag.getId());
        return flag;
    }

    private void bindFlag(java.sql.PreparedStatement statement, FlagEntity flag) throws java.sql.SQLException {
        statement.setString(1, flag.getFlagKey());
        statement.setString(2, flag.getAppKey());
        statement.setString(3, flag.getEnvironment());
        statement.setString(4, flag.getName());
        statement.setString(5, flag.getDescription());
        statement.setString(6, flag.getType());
        statement.setString(7, flag.getDefaultValue());
        statement.setBoolean(8, flag.isEnabled());
        statement.setString(9, flag.getReleaseKey());
        statement.setString(10, flag.getStatus());
        statement.setTimestamp(11, Timestamp.from(flag.getCreatedAt()));
        statement.setTimestamp(12, Timestamp.from(flag.getUpdatedAt()));
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
        flag.setEnabled(rs.getBoolean("enabled"));
        flag.setReleaseKey(rs.getString("release_key"));
        flag.setStatus(rs.getString("status"));
        return flag;
    }
}
