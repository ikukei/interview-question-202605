package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.ApplicationEntity;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationRepository {
    private final JdbcTemplate jdbcTemplate;

    public ApplicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ApplicationEntity> findByAppKey(String appKey) {
        String sql = "select * from ff_application where app_key = ?";
        List<ApplicationEntity> result = jdbcTemplate.query(sql, this::mapRow, appKey);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public List<ApplicationEntity> findAll() {
        String sql = "select * from ff_application order by app_key";
        return jdbcTemplate.query(sql, this::mapRow);
    }

    public ApplicationEntity save(ApplicationEntity app) {
        if (app.getId() == null) {
            return insert(app);
        }
        return update(app);
    }

    private ApplicationEntity insert(ApplicationEntity app) {
        long nextId = jdbcTemplate.queryForObject("select ff_application_seq.nextval from dual", Long.class);
        String sql = "insert into ff_application(id, app_key, name, owner, created_at, updated_at) values (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, nextId, app.getAppKey(), app.getName(), app.getOwner(), Timestamp.from(app.getCreatedAt()), Timestamp.from(app.getUpdatedAt()));
        app.setId(nextId);
        return app;
    }

    private ApplicationEntity update(ApplicationEntity app) {
        String sql = "update ff_application set name = ?, owner = ?, updated_at = ? where id = ?";
        jdbcTemplate.update(sql, app.getName(), app.getOwner(), Timestamp.from(app.getUpdatedAt()), app.getId());
        return app;
    }

    private ApplicationEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        ApplicationEntity app = new ApplicationEntity();
        app.setId(rs.getLong("id"));
        app.setAppKey(rs.getString("app_key"));
        app.setName(rs.getString("name"));
        app.setOwner(rs.getString("owner"));
        return app;
    }
}