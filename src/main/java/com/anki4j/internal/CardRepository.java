package com.anki4j.internal;

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

    public CardRepository(Connection connection) {
        logger.info("Initializing CardRepository");
        this.connection = connection;
    }

    public List<Card> getCards(long deckId) {
        logger.info("Fetching cards for deck ID: {}", deckId);
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
                    cards.add(c);
                }
            }
            logger.info("Found {} cards", cards.size());
        } catch (SQLException e) {
            logger.error("Failed to query cards: {}", e.getMessage());
            throw new AnkiException("Failed to query cards", e);
        }
        return cards;
    }

    public Optional<Card> getCard(long cardId) {
        logger.info("Fetching card with ID: {}", cardId);
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
                    logger.info("Card found: {}", cardId);
                    return Optional.of(c);
                }
            }
            logger.info("Card not found: {}", cardId);
        } catch (SQLException e) {
            logger.error("Failed to query card by ID {}: {}", cardId, e.getMessage());
            throw new AnkiException("Failed to query card by id: " + cardId, e);
        }
        return Optional.empty();
    }

    public void addCard(Card card) {
        logger.info("Adding card to database: {}", card.getId());
        String sql = "INSERT INTO cards (id, nid, did, ord, mod, usn, type, queue, due, ivl, factor, reps, lapses, left, odue, odid, flags, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, card.getId());
            pstmt.setLong(2, card.getNoteId());
            pstmt.setLong(3, card.getDeckId());
            pstmt.setLong(4, card.getOrdinal());
            pstmt.setLong(5, System.currentTimeMillis() / 1000); // mod
            pstmt.setInt(6, -1); // usn
            pstmt.setInt(7, 0); // type
            pstmt.setInt(8, 0); // queue
            pstmt.setLong(9, 0); // due
            pstmt.setInt(10, 0); // ivl
            pstmt.setInt(11, 0); // factor
            pstmt.setInt(12, 0); // reps
            pstmt.setInt(13, 0); // lapses
            pstmt.setInt(14, 0); // left
            pstmt.setLong(15, 0); // odue
            pstmt.setLong(16, 0); // odid
            pstmt.setInt(17, 0); // flags
            pstmt.setString(18, ""); // data
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to add card: {}", e.getMessage());
            throw new AnkiException("Failed to add card", e);
        }
    }
}
