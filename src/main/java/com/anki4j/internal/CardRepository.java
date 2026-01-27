package com.anki4j.internal;

import com.anki4j.AnkiCollection;
import com.anki4j.exception.AnkiException;
import com.anki4j.model.Card;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CardRepository {
    private static final Logger logger = LoggerFactory.getLogger(CardRepository.class);

    private final Connection connection;
    private AnkiCollection context;

    public CardRepository(Connection connection) {
        logger.info("Initializing CardRepository");
        this.connection = connection;
    }

    public void setContext(AnkiCollection context) {
        logger.info("Setting context for CardRepository");
        this.context = context;
    }

    public List<Card> getCards(long deckId) {
        List<Card> cards = new ArrayList<>();
        String sql = "SELECT id, nid, did, ord FROM cards";
        if (deckId != -1) {
            sql += " WHERE did = ?";
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (deckId != -1) {
                stmt.setLong(1, deckId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Card c = new Card(
                            rs.getLong("id"),
                            rs.getLong("nid"),
                            rs.getLong("did"),
                            rs.getLong("ord"));
                    c.setContext(context);
                    cards.add(c);
                }
            }
        } catch (SQLException e) {
            throw new AnkiException("Failed to query cards", e);
        }
        return cards;
    }

    public Optional<Card> getCard(long cardId) {
        String sql = "SELECT id, nid, did, ord FROM cards WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, cardId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Card c = new Card(
                            rs.getLong("id"),
                            rs.getLong("nid"),
                            rs.getLong("did"),
                            rs.getLong("ord"));
                    c.setContext(context);
                    return Optional.of(c);
                }
            }
        } catch (SQLException e) {
            throw new AnkiException("Failed to query card by id: " + cardId, e);
        }
        return Optional.empty();
    }
}
