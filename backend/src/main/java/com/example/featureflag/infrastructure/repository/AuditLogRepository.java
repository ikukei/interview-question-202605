package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.AuditLogEntity;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AuditLogEntity save(AuditLogEntity log) {
        long nextId = jdbcTemplate.queryForObject("select ff_change_event_seq.nextval from dual", Long.class);
        String sql = """
                insert into ff_change_event(id, actor, action, resource_type, resource_key, before_json, after_json, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, nextId, log.getActor(), log.getAction(), log.getResourceType(),
                log.getResourceKey(), log.getBeforeJson(), log.getAfterJson(),
                Timestamp.from(log.getCreatedAt()));
        log.setId(nextId);
        return log;
    }

    public List<AuditLogEntity> findByResourceKeyContaining(String flagKey) {
        String sql = """
                select * from ff_change_event
                where resource_key like ? or resource_key = ?
                order by created_at desc
                """;
        return jdbcTemplate.query(sql, this::mapRow, "%" + flagKey, flagKey);
    }

    private AuditLogEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        AuditLogEntity log = new AuditLogEntity();
        log.setId(rs.getLong("id"));
        log.setActor(rs.getString("actor"));
        log.setAction(rs.getString("action"));
        log.setResourceType(rs.getString("resource_type"));
        log.setResourceKey(rs.getString("resource_key"));
        log.setBeforeJson(rs.getString("before_json"));
        log.setAfterJson(rs.getString("after_json"));
        log.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return log;
    }
}
