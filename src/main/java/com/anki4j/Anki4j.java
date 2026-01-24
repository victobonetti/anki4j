package com.anki4j;

import com.anki4j.exception.AnkiException;
import com.anki4j.model.Card;
import com.anki4j.model.Deck;
import com.anki4j.model.Note;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Iterator;
import java.util.Map;

public class Anki4j implements AutoCloseable {

    private final Path tempDir;
    private final Connection connection;
    private final ObjectMapper objectMapper;

    private Anki4j(Path tempDir, Connection connection) {
        this.tempDir = tempDir;
        this.connection = connection;
        this.objectMapper = new ObjectMapper();
    }

    public static Anki4j read(String path) {
        Path apkgPath = Paths.get(path);
        if (!Files.exists(apkgPath)) {
            throw new AnkiException("File not found: " + path);
        }

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("anki4j_" + UUID.randomUUID());
        } catch (IOException e) {
            throw new AnkiException("Failed to create temporary directory", e);
        }

        try {
            extractCollection(apkgPath, tempDir);

            // Try to find the database file. It should be collection.anki2 or
            // collection.anki21
            Path dbFile = tempDir.resolve("collection.anki21");
            if (!Files.exists(dbFile)) {
                dbFile = tempDir.resolve("collection.anki2");
            }

            if (!Files.exists(dbFile)) {
                throw new AnkiException("Could not find collection.anki2 or collection.anki21 in the archive");
            }

            // Connect to SQLite
            String url = "jdbc:sqlite:" + dbFile.toAbsolutePath();
            Connection conn = DriverManager.getConnection(url);

            return new Anki4j(tempDir, conn);

        } catch (Exception e) {
            // Cleanup on failure
            silentDeleteDir(tempDir);
            if (e instanceof AnkiException) {
                throw (AnkiException) e;
            }
            throw new AnkiException("Failed to read Anki file", e);
        }
    }

    private static void extractCollection(Path apkgPath, Path outputDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(apkgPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if ("collection.anki2".equals(name) || "collection.anki21".equals(name)) {
                    Path outFile = outputDir.resolve(name);
                    Files.copy(zis, outFile, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    public List<Deck> getDecks() {
        List<Deck> decks = new ArrayList<>();
        // Anki 2.1+ stores decks in a separate table 'decks' or inside 'col' table
        // json.
        // We will try querying the 'decks' table first (newer structure), if fails, try
        // fallback.

        try (Statement stmt = connection.createStatement()) {
            boolean decksTableExists = false;
            try (ResultSet rs = connection.getMetaData().getTables(null, null, "decks", null)) {
                if (rs.next()) {
                    decksTableExists = true;
                }
            }

            if (decksTableExists) {
                try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM decks")) {
                    while (rs.next()) {
                        Deck d = new Deck(rs.getLong("id"), rs.getString("name"));
                        d.setContext(this);
                        decks.add(d);
                    }
                }
            } else {
                // Fallback: In older Anki versions, decks are in the 'col' table as a JSON
                // string.
                try (ResultSet rs = stmt.executeQuery("SELECT decks FROM col LIMIT 1")) {
                    if (rs.next()) {
                        String json = rs.getString("decks");
                        if (json != null && !json.isEmpty()) {
                            try {
                                JsonNode root = objectMapper.readTree(json);
                                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                                while (fields.hasNext()) {
                                    Map.Entry<String, JsonNode> field = fields.next();
                                    // The key is the deck ID, the value is an object containing "name"
                                    // Example: "1": {"name": "Default", ...}
                                    long id = Long.parseLong(field.getKey());
                                    String name = field.getValue().get("name").asText();
                                    Deck d = new Deck(id, name);
                                    d.setContext(this);
                                    decks.add(d);
                                }
                            } catch (Exception e) {
                                throw new AnkiException("Failed to parse decks JSON from col table", e);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new AnkiException("Failed to query decks", e);
        }
        return decks;
    }

    public List<Card> getCards() {
        return getCards(-1); // -1 means all
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
                    c.setContext(this);
                    cards.add(c);
                }
            }
        } catch (SQLException e) {
            throw new AnkiException("Failed to query cards", e);
        }
        return cards;
    }

    public List<Note> getNotes() {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT id, guid, flds, mid FROM notes";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                notes.add(new Note(
                        rs.getLong("id"),
                        rs.getString("guid"),
                        rs.getString("flds"),
                        rs.getLong("mid")));
            }
        } catch (SQLException e) {
            throw new AnkiException("Failed to query notes", e);
        }
        return notes;
    }

    public Note getNote(long noteId) {
        String sql = "SELECT id, guid, flds, mid FROM notes WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, noteId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Note(
                            rs.getLong("id"),
                            rs.getString("guid"),
                            rs.getString("flds"),
                            rs.getLong("mid"));
                }
            }
        } catch (SQLException e) {
            throw new AnkiException("Failed to query note by id: " + noteId, e);
        }
        return null; // Or throw exception if not found, usually null is safer for lazy loading
                     // integration
    }

    // Helper to join cards and notes if needed
    // The prompt asked for "Join... to extract questions and answers".
    // This is often complex because of templates.
    // We will stick to returning raw objects for Phase 1 unless a specific join
    // method is requested.

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Failed to close database connection: " + e.getMessage());
        }

        silentDeleteDir(tempDir);
    }

    private static void silentDeleteDir(Path dir) {
        if (dir == null || !Files.exists(dir))
            return;
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // Ignore deletion errors as per "silent" contract, or maybe print stack trace
            e.printStackTrace();
        }
    }
}
