package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.ApplicationEntity;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
        String sql = "insert into ff_application(app_key, name, owner, created_at, updated_at) values (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, app.getAppKey());
            stmt.setString(2, app.getName());
            stmt.setString(3, app.getOwner());
            stmt.setTimestamp(4, Timestamp.from(app.getCreatedAt()));
            stmt.setTimestamp(5, Timestamp.from(app.getUpdatedAt()));
            return stmt;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            app.setId(keyHolder.getKey().longValue());
        }
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
