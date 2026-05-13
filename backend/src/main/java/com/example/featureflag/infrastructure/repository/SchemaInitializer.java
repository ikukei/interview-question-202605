package com.example.featureflag.infrastructure.repository;

import java.sql.Connection;
import java.sql.Statement;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class SchemaInitializer implements InitializingBean {
    private final FeatureDataSource dataSource;

    public SchemaInitializer(FeatureDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    create table if not exists ff_application (
                      id bigint auto_increment primary key,
                      app_key varchar(120) not null unique,
                      name varchar(200) not null,
                      owner varchar(120),
                      created_at timestamp not null,
                      updated_at timestamp not null
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists ff_flag (
                      id bigint auto_increment primary key,
                      flag_key varchar(160) not null,
                      app_key varchar(120) not null,
                      environment varchar(40) not null,
                      name varchar(200) not null,
                      description varchar(1000),
                      type varchar(40) not null,
                      default_value clob not null,
                      enabled boolean not null,
                      release_key varchar(160),
                      status varchar(40) not null,
                      created_at timestamp not null,
                      updated_at timestamp not null
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists ff_rule (
                      id bigint auto_increment primary key,
                      flag_id bigint not null,
                      priority int not null,
                      condition_json clob not null,
                      rollout_percentage int not null,
                      variation_value clob not null,
                      enabled boolean not null,
                      created_at timestamp not null,
                      updated_at timestamp not null
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists ff_config_snapshot (
                      id bigint auto_increment primary key,
                      app_key varchar(120) not null,
                      environment varchar(40) not null,
                      version bigint not null,
                      checksum varchar(128) not null,
                      snapshot_json clob not null,
                      published_by varchar(120) not null,
                      published_at timestamp not null
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists ff_audit_log (
                      id bigint auto_increment primary key,
                      actor varchar(120) not null,
                      action varchar(80) not null,
                      resource_type varchar(80) not null,
                      resource_key varchar(200) not null,
                      before_json clob,
                      after_json clob,
                      created_at timestamp not null
                    )
                    """);
        }
    }
}
