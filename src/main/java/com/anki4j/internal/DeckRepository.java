package com.anki4j.internal;

import com.anki4j.exception.AnkiException;
import com.anki4j.model.Deck;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DeckRepository {
    private static final Logger logger = LoggerFactory.getLogger(DeckRepository.class);

    private final Connection connection;
    private final ObjectMapper objectMapper;

    public DeckRepository(Connection connection) {
        logger.info("Initializing DeckRepository");
        this.connection = connection;
        this.objectMapper = new ObjectMapper();
    }

    public List<Deck> getDecks() {
        logger.info("Fetching all decks from 'col' table");
        List<Deck> decks = new ArrayList<>();

        try (Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT decks FROM col LIMIT 1")) {
                if (rs.next()) {
                    String json = rs.getString("decks");
                    if (json != null && !json.isEmpty()) {
                        JsonNode root = objectMapper.readTree(json);
                        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> field = fields.next();
                            long id = Long.parseLong(field.getKey());
                            String name = field.getValue().get("name").asText();
                            Deck d = new Deck(id, name);
                            decks.add(d);
                        }
                    }
                }
            }
            logger.info("Found {} decks", decks.size());
        } catch (Exception e) {
            logger.error("Failed to query decks from col: {}", e.getMessage());
            throw new AnkiException("Failed to query decks", e);
        }
        return decks;
    }

    public Optional<Deck> getDeck(long deckId) {
        logger.info("Fetching deck with ID: {}", deckId);
        try (Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT decks FROM col LIMIT 1")) {
                if (rs.next()) {
                    String json = rs.getString("decks");
                    if (json != null && !json.isEmpty()) {
                        JsonNode root = objectMapper.readTree(json);
                        JsonNode deckNode = root.get(String.valueOf(deckId));
                        if (deckNode != null) {
                            String name = deckNode.get("name").asText();
                            Deck d = new Deck(deckId, name);
                            logger.info("Deck found: {}", deckId);
                            return Optional.of(d);
                        }
                    }
                }
            }
            logger.info("Deck not found: {}", deckId);
        } catch (Exception e) {
            logger.error("Failed to query deck by ID {}: {}", deckId, e.getMessage());
            throw new AnkiException("Failed to query deck by id: " + deckId, e);
        }
        return Optional.empty();
    }

    public void addDeck(Deck deck) {
        logger.info("Adding deck to col JSON: {}", deck.getName());
        try {
            Map<Long, Deck> decks = new java.util.HashMap<>();
            for (Deck d : getDecks()) {
                decks.put(d.getId(), d);
            }
            decks.put(deck.getId(), deck);

            String json = objectMapper.writeValueAsString(decks);
            String sql = "UPDATE col SET decks = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, json);
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Failed to add deck: {}", e.getMessage());
            throw new AnkiException("Failed to add deck", e);
        }
    }

}
