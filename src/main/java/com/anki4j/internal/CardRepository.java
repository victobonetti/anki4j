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

    public List<Card> getCards() {
        return getCards(-1);
    }

    public List<Card> getCards(long deckId) {
        logger.info("Fetching cards for deck ID: {}", deckId);
        List<Card> cards = new ArrayList<>();
        String sql = "SELECT * FROM cards";
        if (deckId != -1) {
            sql += " WHERE did = ?";
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (deckId != -1) {
                stmt.setLong(1, deckId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Card c = mapResultSetToCard(rs);
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
        String sql = "SELECT * FROM cards WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, cardId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Card c = mapResultSetToCard(rs);
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

    private Card mapResultSetToCard(ResultSet rs) throws SQLException {
        Card c = new Card();
        c.setId(rs.getLong("id"));
        c.setNid(rs.getLong("nid"));
        c.setDid(rs.getLong("did"));
        c.setOrd(rs.getInt("ord"));
        c.setMod(rs.getLong("mod"));
        c.setUsn(rs.getInt("usn"));
        c.setType(rs.getInt("type"));
        c.setQueue(rs.getInt("queue"));
        c.setDue(rs.getLong("due"));
        c.setIvl(rs.getInt("ivl"));
        c.setFactor(rs.getInt("factor"));
        c.setReps(rs.getInt("reps"));
        c.setLapses(rs.getInt("lapses"));
        c.setLeft(rs.getInt("left"));
        c.setOdue(rs.getLong("odue"));
        c.setOdid(rs.getLong("odid"));
        c.setFlags(rs.getInt("flags"));
        c.setData(rs.getString("data"));
        return c;
    }

    public void addCard(Card card) {
        logger.info("Adding card to database: {}", card.getId());
        String sql = "INSERT INTO cards (id, nid, did, ord, mod, usn, type, queue, due, ivl, factor, reps, lapses, left, odue, odid, flags, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, card.getId());
            pstmt.setLong(2, card.getNid());
            pstmt.setLong(3, card.getDid());
            pstmt.setInt(4, card.getOrd());
            pstmt.setLong(5, card.getMod() == 0 ? System.currentTimeMillis() / 1000 : card.getMod());
            pstmt.setInt(6, card.getUsn());
            pstmt.setInt(7, card.getType());
            pstmt.setInt(8, card.getQueue());
            pstmt.setLong(9, card.getDue());
            pstmt.setInt(10, card.getIvl());
            pstmt.setInt(11, card.getFactor());
            pstmt.setInt(12, card.getReps());
            pstmt.setInt(13, card.getLapses());
            pstmt.setInt(14, card.getLeft());
            pstmt.setLong(15, card.getOdue());
            pstmt.setLong(16, card.getOdid());
            pstmt.setInt(17, card.getFlags());
            pstmt.setString(18, card.getData() == null ? "" : card.getData());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to add card: {}", e.getMessage());
            throw new AnkiException("Failed to add card", e);
        }
    }
}
