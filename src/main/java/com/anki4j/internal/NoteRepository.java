package com.anki4j.internal;

import com.anki4j.AnkiCollection;
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
    private AnkiCollection context;

    public NoteRepository(Connection connection, CardRepository cardRepository) {
        logger.info("Initializing NoteRepository");
        this.connection = connection;
        this.cardRepository = cardRepository;
    }

    public void setContext(AnkiCollection context) {
        logger.info("Setting context for NoteRepository");
        this.context = context;
    }

    public Optional<Note> getNote(long noteId) {
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
                    note.setContext(context);
                    return Optional.of(note);
                }
            }
        } catch (SQLException e) {
            throw new AnkiException("Failed to query note by id: " + noteId, e);
        }
        return Optional.empty();
    }

    public Optional<Note> getNoteFromCard(long cardId) {
        return cardRepository.getCard(cardId)
                .flatMap(card -> getNote(card.getNoteId()));
    }

}
