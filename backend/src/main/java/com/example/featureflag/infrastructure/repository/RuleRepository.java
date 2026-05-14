package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.RuleEntity;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RuleRepository {
    private final JdbcTemplate jdbcTemplate;

    public RuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RuleEntity> findByFlagIdOrderByPriorityAsc(Long flagId) {
        String sql = "select * from ff_rule where flag_id = ? order by priority";
        return jdbcTemplate.query(sql, this::mapRow, flagId);
    }

    public RuleEntity save(RuleEntity rule) {
        String sql = """
                insert into ff_rule(flag_id, priority, condition_json, rollout_percentage, variation_value, enabled, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setLong(1, rule.getFlagId());
            stmt.setInt(2, rule.getPriority());
            stmt.setString(3, rule.getConditionJson());
            stmt.setInt(4, rule.getRolloutPercentage());
            stmt.setString(5, rule.getVariationValue());
            stmt.setBoolean(6, rule.isEnabled());
            stmt.setTimestamp(7, Timestamp.from(rule.getCreatedAt()));
            stmt.setTimestamp(8, Timestamp.from(rule.getUpdatedAt()));
            return stmt;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            rule.setId(keyHolder.getKey().longValue());
        }
        return rule;
    }

    private RuleEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        RuleEntity rule = new RuleEntity();
        rule.setId(rs.getLong("id"));
        rule.setFlagId(rs.getLong("flag_id"));
        rule.setPriority(rs.getInt("priority"));
        rule.setConditionJson(rs.getString("condition_json"));
        rule.setRolloutPercentage(rs.getInt("rollout_percentage"));
        rule.setVariationValue(rs.getString("variation_value"));
        rule.setEnabled(rs.getBoolean("enabled"));
        return rule;
    }
}
