package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.RuleEntity;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

@Repository
public class RuleRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insertActor;

    public RuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.insertActor = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("ff_rule")
                .usingGeneratedKeyColumns("id");
    }

    public List<RuleEntity> findByFlagIdOrderByPriorityAsc(Long flagId) {
        String sql = "select * from ff_rule where flag_id = ? order by priority";
        return jdbcTemplate.query(sql, this::mapRow, flagId);
    }

    public RuleEntity save(RuleEntity rule) {
        var params = new java.util.HashMap<String, Object>();
        params.put("flag_id", rule.getFlagId());
        params.put("priority", rule.getPriority());
        params.put("condition_json", rule.getConditionJson());
        params.put("rollout_percentage", rule.getRolloutPercentage());
        params.put("variation_value", rule.getVariationValue());
        params.put("enabled", rule.isEnabled());
        params.put("created_at", Timestamp.from(rule.getCreatedAt()));
        params.put("updated_at", Timestamp.from(rule.getUpdatedAt()));

        Number key = insertActor.executeAndReturnKey(params);
        rule.setId(key.longValue());
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
