package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.RuleEntity;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
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
        long nextId = jdbcTemplate.queryForObject("select ff_rule_seq.nextval from dual", Long.class);
        String sql = """
                insert into ff_rule(id, flag_id, priority, condition_json, rollout_percentage, variation_value, enabled, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, nextId, rule.getFlagId(), rule.getPriority(),
                rule.getConditionJson(), rule.getRolloutPercentage(), rule.getVariationValue(),
                rule.isEnabled() ? 1 : 0, Timestamp.from(rule.getCreatedAt()), Timestamp.from(rule.getUpdatedAt()));
        rule.setId(nextId);
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
        rule.setEnabled(rs.getInt("enabled") == 1);
        return rule;
    }
}