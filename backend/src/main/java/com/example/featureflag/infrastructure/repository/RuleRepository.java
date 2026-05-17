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

    public List<RuleEntity> findByConfigId(Long configId) {
        return jdbcTemplate.query("select * from ff_rule where config_id = ? order by id", this::mapRow, configId);
    }

    public RuleEntity save(RuleEntity rule) {
        return rule.getId() == null ? insert(rule) : update(rule);
    }

    public RuleEntity saveConfigRule(RuleEntity rule) {
        List<RuleEntity> existing = findByConfigId(rule.getConfigId());
        if (!existing.isEmpty()) {
            rule.setId(existing.get(0).getId());
            rule.setCreatedAt(existing.get(0).getCreatedAt());
            rule.touch();
            return update(rule);
        }
        return insert(rule);
    }

    private RuleEntity insert(RuleEntity rule) {
        long nextId = jdbcTemplate.queryForObject("select ff_rule_seq.nextval from dual", Long.class);
        String sql = """
                insert into ff_rule(id, config_id, condition_json, rollout_percentage, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, nextId, rule.getConfigId(),
                rule.getConditionJson(), rule.getRolloutPercentage(),
                Timestamp.from(rule.getCreatedAt()), Timestamp.from(rule.getUpdatedAt()));
        rule.setId(nextId);
        return rule;
    }

    private RuleEntity update(RuleEntity rule) {
        String sql = """
                update ff_rule set config_id = ?, condition_json = ?, rollout_percentage = ?, updated_at = ? where id = ?
                """;
        jdbcTemplate.update(sql, rule.getConfigId(), rule.getConditionJson(),
                rule.getRolloutPercentage(), Timestamp.from(rule.getUpdatedAt()), rule.getId());
        return rule;
    }

    private RuleEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        RuleEntity rule = new RuleEntity();
        rule.setId(rs.getLong("id"));
        long configId = rs.getLong("config_id");
        rule.setConfigId(rs.wasNull() ? null : configId);
        rule.setConditionJson(rs.getString("condition_json"));
        rule.setRolloutPercentage(rs.getInt("rollout_percentage"));
        rule.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return rule;
    }
}
