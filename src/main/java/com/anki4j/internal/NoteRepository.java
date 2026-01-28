package com.anki4j.internal;

import com.anki4j.exception.AnkiException;
import com.anki4j.model.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

public class NoteRepository {
    private static final Logger logger = LoggerFactory.getLogger(NoteRepository.class);

    private final Connection connection;
    private final CardRepository cardRepository;

    public NoteRepository(Connection connection, CardRepository cardRepository) {
        logger.info("Initializing NoteRepository");
        this.connection = connection;
        this.cardRepository = cardRepository;
    }

    public Optional<Note> getNote(long noteId) {
        logger.info("Fetching note with ID: {}", noteId);
        String sql = "SELECT * FROM notes WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, noteId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Note note = mapResultSetToNote(rs);
                    logger.info("Note found: {}", noteId);
                    return Optional.of(note);
                }
            }
            logger.info("Note not found: {}", noteId);
        } catch (SQLException e) {
            logger.error("Failed to query note by ID {}: {}", noteId, e.getMessage());
            throw new AnkiException("Failed to query note by id: " + noteId, e);
        }
        return Optional.empty();
    }

    private Note mapResultSetToNote(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getLong("id"));
        note.setGuid(rs.getString("guid"));
        note.setMid(rs.getLong("mid"));
        note.setMod(rs.getLong("mod"));
        note.setUsn(rs.getInt("usn"));
        note.setTags(rs.getString("tags"));
        note.setFlds(rs.getString("flds"));
        note.setSfld(rs.getString("sfld"));
        note.setCsum(rs.getLong("csum"));
        note.setFlags(rs.getInt("flags"));
        note.setData(rs.getString("data"));
        return note;
    }

    public Optional<Note> getNoteFromCard(long cardId) {
        logger.info("Fetching note associated with card ID: {}", cardId);
        return cardRepository.getCard(cardId)
                .flatMap(card -> getNote(card.getNid()));
    }

    public void addNote(Note note) {
        logger.info("Adding note to database: {}", note.getId());
        String sql = "INSERT INTO notes (id, guid, mid, mod, usn, tags, flds, sfld, csum, flags, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, note.getId());
            pstmt.setString(2, note.getGuid());
            pstmt.setLong(3, note.getMid());
            pstmt.setLong(4, note.getMod() == 0 ? System.currentTimeMillis() / 1000 : note.getMod());
            pstmt.setInt(5, note.getUsn());
            pstmt.setString(6, note.getTags() == null ? "" : note.getTags());
            pstmt.setString(7, note.getFlds());
            pstmt.setString(8, note.getSfld() == null ? "" : note.getSfld());
            pstmt.setLong(9, note.getCsum());
            pstmt.setInt(10, note.getFlags());
            pstmt.setString(11, note.getData() == null ? "" : note.getData());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to add note: {}", e.getMessage());
            throw new AnkiException("Failed to add note", e);
        }
    }
}
