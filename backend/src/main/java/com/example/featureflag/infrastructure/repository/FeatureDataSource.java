package com.example.featureflag.infrastructure.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FeatureDataSource {
    private final String url;
    private final String username;
    private final String password;

    public FeatureDataSource(
            @Value("${feature.datasource.driver-class-name:org.h2.Driver}") String driverClassName,
            @Value("${feature.datasource.url}") String url,
            @Value("${feature.datasource.username:sa}") String username,
            @Value("${feature.datasource.password:}") String password
    ) throws ClassNotFoundException {
        Class.forName(driverClassName);
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
