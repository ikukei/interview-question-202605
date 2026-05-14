package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.AuditLogEntity;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AuditLogEntity save(AuditLogEntity log) {
        String sql = """
                insert into ff_audit_log(actor, action, resource_type, resource_key, before_json, after_json, created_at)
                values (?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, log.getActor(), log.getAction(), log.getResourceType(), 
                log.getResourceKey(), log.getBeforeJson(), log.getAfterJson(), 
                Timestamp.from(log.getCreatedAt()));
        return log;
    }
}
