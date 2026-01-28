package com.anki4j.internal;

import com.anki4j.exception.AnkiException;
import com.anki4j.model.Grave;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GraveRepository {
    private static final Logger logger = LoggerFactory.getLogger(GraveRepository.class);
    private final Connection connection;

    public GraveRepository(Connection connection) {
        this.connection = connection;
    }

    public List<Grave> getAllGraves() {
        logger.info("Fetching all graves");
        List<Grave> list = new ArrayList<>();
        String sql = "SELECT * FROM graves";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToGrave(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to query all graves: {}", e.getMessage());
            throw new AnkiException("Failed to query all graves", e);
        }
        return list;
    }

    public Optional<Grave> getGraveByOid(long oid) {
        logger.info("Fetching grave for original ID: {}", oid);
        String sql = "SELECT * FROM graves WHERE oid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, oid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToGrave(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to query grave by OID {}: {}", oid, e.getMessage());
            throw new AnkiException("Failed to query grave", e);
        }
        return Optional.empty();
    }

    private Grave mapResultSetToGrave(ResultSet rs) throws SQLException {
        Grave g = new Grave();
        g.setUsn(rs.getInt("usn"));
        g.setOid(rs.getLong("oid"));
        g.setType(rs.getInt("type"));
        return g;
    }
}
