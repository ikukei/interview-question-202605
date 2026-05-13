package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.RuleEntity;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class RuleRepository {
    private final FeatureDataSource dataSource;

    public RuleRepository(FeatureDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<RuleEntity> findByFlagIdOrderByPriorityAsc(Long flagId) {
        String sql = "select * from ff_rule where flag_id = ? order by priority";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, flagId);
            try (ResultSet rs = statement.executeQuery()) {
                List<RuleEntity> rules = new ArrayList<>();
                while (rs.next()) {
                    rules.add(map(rs));
                }
                return rules;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to list rules", ex);
        }
    }

    public RuleEntity save(RuleEntity rule) {
        String sql = """
                insert into ff_rule(flag_id, priority, condition_json, rollout_percentage, variation_value, enabled, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, rule.getFlagId());
            statement.setInt(2, rule.getPriority());
            statement.setString(3, rule.getConditionJson());
            statement.setInt(4, rule.getRolloutPercentage());
            statement.setString(5, rule.getVariationValue());
            statement.setBoolean(6, rule.isEnabled());
            statement.setTimestamp(7, Timestamp.from(rule.getCreatedAt()));
            statement.setTimestamp(8, Timestamp.from(rule.getUpdatedAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    rule.setId(keys.getLong(1));
                }
            }
            return rule;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create rule", ex);
        }
    }

    private RuleEntity map(ResultSet rs) throws Exception {
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
