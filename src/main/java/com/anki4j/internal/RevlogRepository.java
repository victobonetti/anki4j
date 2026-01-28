package com.anki4j.internal;

import com.anki4j.exception.AnkiException;
import com.anki4j.model.Revlog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RevlogRepository {
    private static final Logger logger = LoggerFactory.getLogger(RevlogRepository.class);
    private final Connection connection;

    public RevlogRepository(Connection connection) {
        this.connection = connection;
    }

    public List<Revlog> getAllRevlogs() {
        logger.info("Fetching all revlogs");
        List<Revlog> list = new ArrayList<>();
        String sql = "SELECT * FROM revlog";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToRevlog(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to query all revlogs: {}", e.getMessage());
            throw new AnkiException("Failed to query all revlogs", e);
        }
        return list;
    }

    public Optional<Revlog> getRevlog(long id) {
        logger.info("Fetching revlog ID: {}", id);
        String sql = "SELECT * FROM revlog WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRevlog(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to query revlog by ID {}: {}", id, e.getMessage());
            throw new AnkiException("Failed to query revlog", e);
        }
        return Optional.empty();
    }

    private Revlog mapResultSetToRevlog(ResultSet rs) throws SQLException {
        Revlog r = new Revlog();
        r.setId(rs.getLong("id"));
        r.setCid(rs.getLong("cid"));
        r.setUsn(rs.getInt("usn"));
        r.setEase(rs.getInt("ease"));
        r.setIvl(rs.getInt("ivl"));
        r.setLastIvl(rs.getInt("lastIvl"));
        r.setFactor(rs.getInt("factor"));
        r.setTime(rs.getInt("time"));
        r.setType(rs.getInt("type"));
        return r;
    }
}
