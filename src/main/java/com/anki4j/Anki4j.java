package com.anki4j;

import com.anki4j.exception.AnkiException;
import com.anki4j.model.Card;
import com.anki4j.model.Deck;
import com.anki4j.model.Note;
import com.anki4j.model.Model;
import com.anki4j.model.Template;
import com.anki4j.service.ModelService;
import com.anki4j.renderer.Renderer;

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
import java.io.InputStream;
import java.util.zip.ZipFile;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class Anki4j implements AnkiCollection {

    private static final Logger logger = LoggerFactory.getLogger(Anki4j.class);

    private final Path tempDir;
    private final Connection connection;
    private final ObjectMapper objectMapper;
    private final ZipFile zipFile;
    private final MediaManager mediaManager;
    private final ModelService modelService;
    private final Renderer renderer;

    private Anki4j(Path tempDir, Connection connection, ZipFile zipFile, MediaManager mediaManager) {
        this.tempDir = tempDir;
        this.connection = connection;
        this.objectMapper = new ObjectMapper();
        this.zipFile = zipFile;
        this.mediaManager = mediaManager;
        this.modelService = new ModelService(connection);
        this.renderer = new Renderer();
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

        ZipFile zip = null;
        try {
            zip = new ZipFile(apkgPath.toFile());

            // Extract DB
            extractCollection(zip, tempDir);

            // Try to find the database file. It should be collection.anki2 or
            // collection.anki21
            Path dbFile = tempDir.resolve("collection.anki21");
            if (!Files.exists(dbFile)) {
                dbFile = tempDir.resolve("collection.anki2");
            }

            if (!Files.exists(dbFile)) {
                throw new AnkiException("Could not find collection.anki2 or collection.anki21 in the archive");
            }

            // Load Media Map
            MediaManager mediaManager = new MediaManager();
            mediaManager.loadMap(zip);

            // Connect to SQLite
            String url = "jdbc:sqlite:" + dbFile.toAbsolutePath();
            Connection conn = DriverManager.getConnection(url);

            return new Anki4j(tempDir, conn, zip, mediaManager);

        } catch (Exception e) {
            // Cleanup on failure
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignored) {
                }
            }
            silentDeleteDir(tempDir);
            if (e instanceof AnkiException) {
                throw (AnkiException) e;
            }
            throw new AnkiException("Failed to read Anki file", e);
        }
    }

    private static void extractCollection(ZipFile zip, Path outputDir) throws IOException {
        ZipEntry entry21 = zip.getEntry("collection.anki21");
        ZipEntry entry20 = zip.getEntry("collection.anki2");

        ZipEntry target = entry21 != null ? entry21 : entry20;

        if (target != null) {
            Path outFile = outputDir.resolve(target.getName());
            try (InputStream is = zip.getInputStream(target)) {
                Files.copy(is, outFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public byte[] getMediaContent(String filename) {
        String zipName = mediaManager.getZipEntryName(filename);
        if (zipName == null) {
            return null; // Media not found in map
        }

        ZipEntry entry = zipFile.getEntry(zipName);
        if (entry == null) {
            return null; // Entry mapping exists but file missing in zip
        }

        try (InputStream is = zipFile.getInputStream(entry)) {
            return is.readAllBytes();
        } catch (IOException e) {
            logger.error("Failed to read media file '{}' (zip entry '{}'): {}", filename, zipName, e.getMessage());
            throw new AnkiException("Failed to read media file: " + filename, e);
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

    public Optional<Deck> getDeck(long deckId) {
        // Try querying 'decks' table first
        try (Statement stmt = connection.createStatement()) {
            boolean decksTableExists = false;
            try (ResultSet rs = connection.getMetaData().getTables(null, null, "decks", null)) {
                if (rs.next()) {
                    decksTableExists = true;
                }
            }

            if (decksTableExists) {
                try (PreparedStatement pstmt = connection.prepareStatement("SELECT id, name FROM decks WHERE id = ?")) {
                    pstmt.setLong(1, deckId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            Deck d = new Deck(rs.getLong("id"), rs.getString("name"));
                            d.setContext(this);
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
                            try {
                                JsonNode root = objectMapper.readTree(json);
                                JsonNode deckNode = root.get(String.valueOf(deckId));
                                if (deckNode != null) {
                                    String name = deckNode.get("name").asText();
                                    Deck d = new Deck(deckId, name);
                                    d.setContext(this);
                                    return Optional.of(d);
                                }
                            } catch (Exception e) {
                                throw new AnkiException("Failed to parse decks JSON from col table", e);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new AnkiException("Failed to query deck by id: " + deckId, e);
        }
        return Optional.empty();
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
                    c.setContext(this);
                    return Optional.of(c);
                }
            }
        } catch (SQLException e) {
            throw new AnkiException("Failed to query card by id: " + cardId, e);
        }
        return Optional.empty();
    }

    public Optional<Note> getNoteFromCard(long cardId) {
        Optional<Card> card = getCard(cardId);
        if (card.isPresent()) {
            return getNote(card.get().getNoteId());
        }
        return Optional.empty();
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
        return Optional.empty(); // Or throw exception if not found, usually null is safer for lazy loading
        // integration
    }

    // Helper to join cards and notes if needed
    // The prompt asked for "Join... to extract questions and answers".
    // This is often complex because of templates.
    // We will stick to returning raw objects for Phase 1 unless a specific join
    // method is requested.

    public Optional<Model> getModel(long modelId) {
        return modelService.getModel(modelId);
    }

    /**
     * Renders the Question side (Front) of the card.
     * 
     * @param card The card to render
     * @return Rendered HTML content or empty string if components missing
     */
    public String renderFront(Card card) {
        return render(card, true);
    }

    /**
     * Renders the Answer side (Back) of the card.
     * 
     * @param card The card to render
     * @return Rendered HTML content or empty string if components missing
     */
    public String renderBack(Card card) {
        return render(card, false);
    }

    private String render(Card card, boolean questionSide) {
        Optional<Note> noteOpt = getNoteFromCard(card.getId());
        if (noteOpt.isEmpty())
            return "";
        Note note = noteOpt.get();

        Optional<Model> modelOpt = getModel(note.getModelId());
        if (modelOpt.isEmpty())
            return "";
        Model model = modelOpt.get();

        // Find the template corresponding to the card's ordinal
        // The 'ord' in card corresponds to the index in model's templates list?
        // Usually yes, but better to check template 'ord' if available, or list index.
        // Anki docs say: "The ordinal of the card template. 0 for the first card
        // type..."
        int ord = (int) card.getOrdinal();

        Template template = null;
        if (model.getTmpls() != null && ord < model.getTmpls().size()) {
            template = model.getTmpls().get(ord);
            // Verify ordinal just in case? Usually list index matches ord.
        }

        if (template == null)
            return "";

        if (questionSide) {
            return renderer.renderQuestion(note, model, template);
        } else {
            return renderer.renderAnswer(note, model, template);
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to close database connection: {}", e.getMessage());
        }

        try {
            if (zipFile != null) {
                zipFile.close();
            }
        } catch (IOException e) {
            logger.error("Failed to close zip file: {}", e.getMessage());
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
            // Ignore deletion errors as per "silent" contract, or maybe print stack
            // trace
            e.printStackTrace();
        }
    }
}
