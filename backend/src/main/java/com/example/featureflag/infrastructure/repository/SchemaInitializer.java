package com.example.featureflag.infrastructure.repository;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaInitializer {
    private final JdbcTemplate jdbcTemplate;

    public SchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    private void initializeSchema() {
        jdbcTemplate.execute("""
                create table if not exists ff_application (
                  id number(19) primary key,
                  app_key varchar(120) not null unique,
                  name varchar(200) not null,
                  owner varchar(120),
                  created_at timestamp not null,
                  updated_at timestamp not null
                )
                """);
        jdbcTemplate.execute("create sequence if not exists ff_application_seq start with 1 increment by 1");
        
        jdbcTemplate.execute("""
                create table if not exists ff_flag (
                  id number(19) primary key,
                  flag_key varchar(160) not null,
                  app_key varchar(120) not null,
                  environment varchar(40) not null,
                  name varchar(200) not null,
                  description varchar(1000),
                  type varchar(40) not null,
                  default_value clob not null,
                  enabled number(1) not null,
                  release_key varchar(160),
                  status varchar(40) not null,
                  created_at timestamp not null,
                  updated_at timestamp not null
                )
                """);
        jdbcTemplate.execute("create sequence if not exists ff_flag_seq start with 1 increment by 1");

        jdbcTemplate.execute("""
                create table if not exists ff_flag_config (
                  id number(19) primary key,
                  flag_id number(19) not null,
                  app_key varchar(120) not null,
                  environment varchar(40) not null,
                  enabled number(1) not null,
                  release_key varchar(160),
                  rollout_percentage int not null,
                  status varchar(40) not null,
                  created_at timestamp not null,
                  updated_at timestamp not null
                )
                """);
        jdbcTemplate.execute("create sequence if not exists ff_flag_config_seq start with 1 increment by 1");
        
        jdbcTemplate.execute("""
                create table if not exists ff_rule (
                  id number(19) primary key,
                  flag_id number(19) not null,
                  config_id number(19),
                  priority int not null,
                  condition_json clob not null,
                  rollout_percentage int not null,
                  variation_value clob not null,
                  enabled number(1) not null,
                  created_at timestamp not null,
                  updated_at timestamp not null
                )
                """);
        jdbcTemplate.execute("create sequence if not exists ff_rule_seq start with 1 increment by 1");
        jdbcTemplate.execute("alter table ff_rule add column if not exists config_id number(19)");
        
        jdbcTemplate.execute("""
                create table if not exists ff_config_snapshot (
                  id number(19) primary key,
                  app_key varchar(120) not null,
                  environment varchar(40) not null,
                  version number(19) not null,
                  checksum varchar(128) not null,
                  snapshot_json clob not null,
                  published_by varchar(120) not null,
                  published_at timestamp not null
                )
                """);
        jdbcTemplate.execute("create sequence if not exists ff_config_snapshot_seq start with 1 increment by 1");
        
        jdbcTemplate.execute("""
                create table if not exists ff_change_event (
                  id number(19) primary key,
                  actor varchar(120) not null,
                  action varchar(80) not null,
                  resource_type varchar(80) not null,
                  resource_key varchar(200) not null,
                  before_json clob,
                  after_json clob,
                  created_at timestamp not null
                )
                """);
        jdbcTemplate.execute("create sequence if not exists ff_change_event_seq start with 1 increment by 1");
    }
}
