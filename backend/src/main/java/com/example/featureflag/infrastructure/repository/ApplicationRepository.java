package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.ApplicationEntity;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insertActor;

    public ApplicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.insertActor = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("ff_application")
                .usingGeneratedKeyColumns("id");
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
        var params = new java.util.HashMap<String, Object>();
        params.put("app_key", app.getAppKey());
        params.put("name", app.getName());
        params.put("owner", app.getOwner());
        params.put("created_at", Timestamp.from(app.getCreatedAt()));
        params.put("updated_at", Timestamp.from(app.getUpdatedAt()));

        Number key = insertActor.executeAndReturnKey(params);
        app.setId(key.longValue());
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
