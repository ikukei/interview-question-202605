package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.ApplicationEntity;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationRepository {
    private final FeatureDataSource dataSource;

    public ApplicationRepository(FeatureDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<ApplicationEntity> findByAppKey(String appKey) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("select * from ff_application where app_key = ?")) {
            statement.setString(1, appKey);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to find application", ex);
        }
    }

    public List<ApplicationEntity> findAll() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("select * from ff_application order by app_key");
             ResultSet rs = statement.executeQuery()) {
            List<ApplicationEntity> apps = new ArrayList<>();
            while (rs.next()) {
                apps.add(map(rs));
            }
            return apps;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to list applications", ex);
        }
    }

    public ApplicationEntity save(ApplicationEntity app) {
        if (app.getId() == null) {
            return insert(app);
        }
        return update(app);
    }

    private ApplicationEntity insert(ApplicationEntity app) {
        String sql = "insert into ff_application(app_key, name, owner, created_at, updated_at) values (?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, app.getAppKey());
            statement.setString(2, app.getName());
            statement.setString(3, app.getOwner());
            statement.setTimestamp(4, Timestamp.from(app.getCreatedAt()));
            statement.setTimestamp(5, Timestamp.from(app.getUpdatedAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    app.setId(keys.getLong(1));
                }
            }
            return app;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create application", ex);
        }
    }

    private ApplicationEntity update(ApplicationEntity app) {
        String sql = "update ff_application set name = ?, owner = ?, updated_at = ? where id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, app.getName());
            statement.setString(2, app.getOwner());
            statement.setTimestamp(3, Timestamp.from(app.getUpdatedAt()));
            statement.setLong(4, app.getId());
            statement.executeUpdate();
            return app;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to update application", ex);
        }
    }

    private ApplicationEntity map(ResultSet rs) throws Exception {
        ApplicationEntity app = new ApplicationEntity();
        app.setId(rs.getLong("id"));
        app.setAppKey(rs.getString("app_key"));
        app.setName(rs.getString("name"));
        app.setOwner(rs.getString("owner"));
        return app;
    }
}
