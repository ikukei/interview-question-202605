package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.AuditLogEntity;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository {
    private final FeatureDataSource dataSource;

    public AuditLogRepository(FeatureDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public AuditLogEntity save(AuditLogEntity log) {
        String sql = """
                insert into ff_audit_log(actor, action, resource_type, resource_key, before_json, after_json, created_at)
                values (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, log.getActor());
            statement.setString(2, log.getAction());
            statement.setString(3, log.getResourceType());
            statement.setString(4, log.getResourceKey());
            statement.setString(5, log.getBeforeJson());
            statement.setString(6, log.getAfterJson());
            statement.setTimestamp(7, Timestamp.from(log.getCreatedAt()));
            statement.executeUpdate();
            return log;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to save audit log", ex);
        }
    }
}
