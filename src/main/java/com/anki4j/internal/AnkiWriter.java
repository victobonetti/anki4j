package com.anki4j.internal;

import com.anki4j.exception.AnkiException;
import com.anki4j.model.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AnkiWriter {
    private static final Logger logger = LoggerFactory.getLogger(AnkiWriter.class);

    private final Connection connection;

    public AnkiWriter(Connection connection) {
        logger.info("Initializing AnkiWriter");
        this.connection = connection;
    }

    public void save(Note note) {
        if (!note.isDirty()) {
            logger.info("Note ID {} is not dirty, skipping save", note.getId());
            return;
        }

        logger.info("Saving changes for note ID: {}", note.getId());
        String rawFields = note.getFlds();
        long mod = System.currentTimeMillis();

        String sql = "UPDATE notes SET flds = ?, mod = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, rawFields);
            pstmt.setLong(2, mod);
            pstmt.setLong(3, note.getId());

            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                logger.error("Failed to update note ID {}: Note not found", note.getId());
                throw new AnkiException("Failed to update note: Note with ID " + note.getId() + " not found.");
            }

            logger.info("Successfully updated note ID {} in database", note.getId());

            // Reset dirty flag after successful save
            note.setDirty(false);

        } catch (SQLException e) {
            logger.error("Error saving note ID {}: {}", note.getId(), e.getMessage());
            throw new AnkiException("Failed to save note to database", e);
        }
    }
}
