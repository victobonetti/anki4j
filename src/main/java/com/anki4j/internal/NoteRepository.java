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
        String sql = "SELECT id, guid, flds, mid FROM notes WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, noteId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Note note = new Note(
                            rs.getLong("id"),
                            rs.getString("guid"),
                            rs.getString("flds"),
                            rs.getLong("mid"));
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

    public Optional<Note> getNoteFromCard(long cardId) {
        logger.info("Fetching note associated with card ID: {}", cardId);
        return cardRepository.getCard(cardId)
                .flatMap(card -> getNote(card.getNoteId()));
    }

    public void addNote(Note note) {
        logger.info("Adding note to database: {}", note.getId());
        String sql = "INSERT INTO notes (id, guid, mid, mod, usn, tags, flds, sfld, csum, flags, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, note.getId());
            pstmt.setString(2, note.getGuid());
            pstmt.setLong(3, note.getModelId());
            pstmt.setLong(4, System.currentTimeMillis() / 1000); // mod
            pstmt.setInt(5, -1); // usn
            pstmt.setString(6, ""); // tags
            pstmt.setString(7, note.getFields());
            pstmt.setLong(8, 0); // sfld
            pstmt.setLong(9, 0); // csum
            pstmt.setInt(10, 0); // flags
            pstmt.setString(11, ""); // data
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to add note: {}", e.getMessage());
            throw new AnkiException("Failed to add note", e);
        }
    }
}
