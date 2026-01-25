package com.anki4j.internal;

import com.anki4j.exception.AnkiException;
import com.anki4j.model.Note;

import java.sql.*;
import java.util.Optional;

public class NoteRepository {

    private final Connection connection;
    private final CardRepository cardRepository;

    public NoteRepository(Connection connection, CardRepository cardRepository) {
        this.connection = connection;
        this.cardRepository = cardRepository;
    }

    public Optional<Note> getNote(long noteId) {
        String sql = "SELECT id, guid, flds, mid FROM notes WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, noteId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Note(
                            rs.getLong("id"),
                            rs.getString("guid"),
                            rs.getString("flds"),
                            rs.getLong("mid")));
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
