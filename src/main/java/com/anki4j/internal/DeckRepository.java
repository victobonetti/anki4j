package com.anki4j.internal;

import com.anki4j.AnkiCollection;
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
    private AnkiCollection context;

    public DeckRepository(Connection connection) {
        logger.info("Initializing DeckRepository");
        this.connection = connection;
        this.objectMapper = new ObjectMapper();
    }

    public void setContext(AnkiCollection context) {
        logger.info("Setting context for DeckRepository");
        this.context = context;
    }

    public List<Deck> getDecks() {
        logger.info("Fetching all decks");
        List<Deck> decks = new ArrayList<>();

        try (Statement stmt = connection.createStatement()) {
            boolean decksTableExists = tableExists("decks");

            if (decksTableExists) {
                logger.info("Reading decks from 'decks' table");
                try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM decks")) {
                    while (rs.next()) {
                        Deck d = new Deck(rs.getLong("id"), rs.getString("name"));
                        d.setContext(context);
                        decks.add(d);
                    }
                }
            } else {
                // Fallback: In older Anki versions, decks are in the 'col' table as a JSON
                // string.
                logger.info("Reading decks from 'col' table (legacy mode)");
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
                                d.setContext(context);
                                decks.add(d);
                            }
                        }
                    }
                }
            }
            logger.info("Found {} decks", decks.size());
        } catch (Exception e) {
            logger.error("Failed to query decks: {}", e.getMessage());
            throw new AnkiException("Failed to query decks", e);
        }
        return decks;
    }

    public Optional<Deck> getDeck(long deckId) {
        logger.info("Fetching deck with ID: {}", deckId);
        try (Statement stmt = connection.createStatement()) {
            boolean decksTableExists = tableExists("decks");

            if (decksTableExists) {
                try (PreparedStatement pstmt = connection.prepareStatement("SELECT id, name FROM decks WHERE id = ?")) {
                    pstmt.setLong(1, deckId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            Deck d = new Deck(rs.getLong("id"), rs.getString("name"));
                            d.setContext(context);
                            logger.info("Deck found: {}", deckId);
                            return Optional.of(d);
                        }
                    }
                }
            } else {
                // Fallback: Parse 'col' table JSON
                try (ResultSet rs = stmt.executeQuery("SELECT decks FROM col LIMIT 1")) {
                    if (rs.next()) {
                        String json = rs.getString("decks");
                        if (json != null && !json.isEmpty()) {
                            JsonNode root = objectMapper.readTree(json);
                            JsonNode deckNode = root.get(String.valueOf(deckId));
                            if (deckNode != null) {
                                String name = deckNode.get("name").asText();
                                Deck d = new Deck(deckId, name);
                                d.setContext(context);
                                logger.info("Deck found: {}", deckId);
                                return Optional.of(d);
                            }
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

    private boolean tableExists(String tableName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }
}
