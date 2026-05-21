package com.example.featureflag.infrastructure.repository;

import com.example.featureflag.domain.FlagEntity;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FlagRepository {
    private final JdbcTemplate jdbcTemplate;

    public FlagRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FlagEntity> findAllOrderByFlagKeyAsc() {
        String sql = "select * from ff_flag order by flag_key";
        return jdbcTemplate.query(sql, this::mapRow);
    }

    public Optional<FlagEntity> findById(Long id) {
        String sql = "select * from ff_flag where id = ?";
        List<FlagEntity> result = jdbcTemplate.query(sql, this::mapRow, id);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public Optional<FlagEntity> findByFlagKey(String flagKey) {
        String sql = "select * from ff_flag where flag_key = ?";
        List<FlagEntity> result = jdbcTemplate.query(sql, this::mapRow, flagKey);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public FlagEntity save(FlagEntity flag) {
        return flag.getId() == null ? insert(flag) : update(flag);
    }

    private FlagEntity insert(FlagEntity flag) {
        long nextId = jdbcTemplate.queryForObject("select ff_flag_seq.nextval from dual", Long.class);
        String sql = """
                insert into ff_flag(id, flag_key, description, type, release_key, enabled, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, nextId, flag.getFlagKey(),
                flag.getDescription(), flag.getType(), flag.getReleaseKey(), flag.isEnabled() ? 1 : 0,
                Timestamp.from(flag.getCreatedAt()), Timestamp.from(flag.getUpdatedAt()));
        flag.setId(nextId);
        return flag;
    }

    private FlagEntity update(FlagEntity flag) {
        String sql = """
                update ff_flag set flag_key = ?, description = ?, type = ?,
                  release_key = ?, enabled = ?, updated_at = ? where id = ?
                """;
        jdbcTemplate.update(sql, flag.getFlagKey(),
                flag.getDescription(), flag.getType(), flag.getReleaseKey(), flag.isEnabled() ? 1 : 0,
                Timestamp.from(flag.getUpdatedAt()), flag.getId());
        return flag;
    }

    private FlagEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        FlagEntity flag = new FlagEntity();
        flag.setId(rs.getLong("id"));
        flag.setFlagKey(rs.getString("flag_key"));
        flag.setDescription(rs.getString("description"));
        flag.setType(rs.getString("type"));
        flag.setReleaseKey(rs.getString("release_key"));
        flag.setEnabled(rs.getInt("enabled") == 1);
        flag.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        flag.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return flag;
    }
}
