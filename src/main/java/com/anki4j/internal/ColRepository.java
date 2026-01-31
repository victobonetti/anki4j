package com.anki4j.internal;

import com.anki4j.model.Col;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ColRepository {
    private static final Logger logger = LoggerFactory.getLogger(ColRepository.class);
    private final Connection connection;

    public ColRepository(Connection connection) {
        this.connection = connection;
    }

    public Optional<Col> getCol() {
        String sql = "SELECT * FROM col LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                Col col = new Col();
                col.setId(rs.getLong("id"));
                col.setCrt(rs.getLong("crt"));
                col.setMod(rs.getLong("mod"));
                col.setScm(rs.getLong("scm"));
                col.setVer(rs.getInt("ver"));
                col.setDty(rs.getInt("dty"));
                col.setUsn(rs.getInt("usn"));
                col.setLs(rs.getLong("ls"));
                col.setConf(rs.getString("conf"));
                col.setModels(rs.getString("models"));
                col.setDecks(rs.getString("decks"));
                col.setDconf(rs.getString("dconf"));
                col.setTags(rs.getString("tags"));
                return Optional.of(col);
            }
        } catch (SQLException e) {
            logger.error("Failed to query collection settings: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
