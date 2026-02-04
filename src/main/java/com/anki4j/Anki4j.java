package com.anki4j;

import com.anki4j.exception.AnkiException;
import com.anki4j.internal.*;
import com.anki4j.model.Card;
import com.anki4j.model.Deck;
import com.anki4j.model.Model;
import com.anki4j.model.Note;
import com.anki4j.model.Grave;
import com.anki4j.model.Revlog;
import com.anki4j.renderer.RenderedCard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class Anki4j implements AnkiCollection {

    private static final Logger logger = LoggerFactory.getLogger(Anki4j.class);

    // Resource handles
    private final java.nio.file.Path originalPath;
    private final java.sql.Connection connection;

    // Services
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final NoteRepository noteRepository;
    private final ModelService modelService;
    private final MediaManager mediaManager;
    private final RenderService renderService;
    private final AnkiWriter ankiWriter;
    private final RevlogRepository revlogRepository;
    private final GraveRepository graveRepository;
    private final ColRepository colRepository;

    private boolean dirty = false;

    private Anki4j(java.nio.file.Path originalPath, java.sql.Connection connection,
            DeckRepository deckRepository, CardRepository cardRepository,
            NoteRepository noteRepository, ModelService modelService,
            MediaManager mediaManager, RenderService renderService,
            AnkiWriter ankiWriter, RevlogRepository revlogRepository,
            GraveRepository graveRepository, ColRepository colRepository) {
        logger.info("Initializing Anki4j instance");
        this.originalPath = originalPath;
        this.connection = connection;
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.noteRepository = noteRepository;
        this.modelService = modelService;
        this.mediaManager = mediaManager;
        this.renderService = renderService;
        this.ankiWriter = ankiWriter;
        this.revlogRepository = revlogRepository;
        this.graveRepository = graveRepository;
        this.colRepository = colRepository;
    }

    public static Anki4j read(String path) {
        logger.info("Opening Anki file from path: {}", path);
        java.nio.file.Path apkgPath = java.nio.file.Paths.get(path);
        try {
            byte[] data = java.nio.file.Files.readAllBytes(apkgPath);
            Anki4j instance = read(data);
            return new Anki4j(apkgPath, instance.connection,
                    instance.deckRepository, instance.cardRepository, instance.noteRepository,
                    instance.modelService, instance.mediaManager, instance.renderService,
                    instance.ankiWriter, instance.revlogRepository, instance.graveRepository,
                    instance.colRepository);
        } catch (IOException e) {
            throw new AnkiException("Failed to read Anki file from path: " + path, e);
        }
    }

    public static Anki4j read(java.io.InputStream inputStream) {
        try {
            return read(inputStream.readAllBytes());
        } catch (IOException e) {
            throw new AnkiException("Failed to read Anki data from InputStream", e);
        }
    }

    // ... (existing helper methods)

    public static Anki4j read(byte[] data) {
        logger.info("Opening Anki collection from bytes (length: {})", data.length);

        try {
            // 1. Extract database from bytes
            byte[] dbBytes = DatabaseManager.extractDatabaseBytes(data);
            if (dbBytes == null) {
                throw new AnkiException("Invalid Anki package: collection.anki2 or collection.anki21 not found");
            }

            // 2. Connect to in-memory SQLite and load data
            java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite::memory:");
            DatabaseManager.restore(conn, dbBytes);

            return initializeFromConnection(conn, data);

        } catch (Exception e) {
            if (e instanceof AnkiException) {
                throw (AnkiException) e;
            }
            throw new AnkiException("Failed to initialize Anki4j from bytes", e);
        }
    }

    public static Anki4j create() {
        logger.info("Creating new empty Anki collection");
        try {
            java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite::memory:");
            DatabaseManager.initializeSchema(conn);

            return initializeFromConnection(conn, null);
        } catch (Exception e) {
            throw new AnkiException("Failed to create new Anki collection", e);
        }
    }

    private static Anki4j initializeFromConnection(java.sql.Connection conn, byte[] data) throws IOException {
        logger.info("Initializing services from connection");
        MediaManager mediaManager = new MediaManager();
        if (data != null) {
            mediaManager.load(data);
        }

        CardRepository cardRepository = new CardRepository(conn);
        NoteRepository noteRepository = new NoteRepository(conn, cardRepository);
        DeckRepository deckRepository = new DeckRepository(conn);
        ModelService modelService = new ModelService(conn);
        RenderService renderService = new RenderService(noteRepository, modelService);
        AnkiWriter ankiWriter = new AnkiWriter(conn);
        RevlogRepository revlogRepository = new RevlogRepository(conn);
        GraveRepository graveRepository = new GraveRepository(conn);
        ColRepository colRepository = new ColRepository(conn);

        return new Anki4j(null, conn,
                deckRepository, cardRepository, noteRepository,
                modelService, mediaManager, renderService, ankiWriter,
                revlogRepository, graveRepository, colRepository);
    }

    // ==================== Delegated Methods ====================

    @Override
    public List<Deck> getDecks() {
        return deckRepository.getDecks();
    }

    @Override
    public Optional<Deck> getDeck(long deckId) {
        return deckRepository.getDeck(deckId);
    }

    @Override
    public List<Card> getCards() {
        return cardRepository.getCards();
    }

    @Override
    public List<Card> getCards(long deckId) {
        logger.debug("Fetching cards for deck ID: {}", deckId);
        return cardRepository.getCards(deckId);
    }

    @Override
    public Optional<Card> getCard(long cardId) {
        return cardRepository.getCard(cardId);
    }

    @Override
    public Optional<Note> getNote(long noteId) {
        return noteRepository.getNote(noteId);
    }

    @Override
    public List<Note> getNotes() {
        return noteRepository.getNotes();
    }

    @Override
    public Optional<Note> getNoteFromCard(long cardId) {
        return noteRepository.getNoteFromCard(cardId);
    }

    @Override
    public Optional<byte[]> getMediaContent(String filename) {
        return mediaManager.getMediaContent(filename);
    }

    @Override
    public List<Model> getModels() {
        return modelService.getAllModels();
    }

    @Override
    public Optional<Model> getModel(long modelId) {
        return modelService.getModel(modelId);
    }

    @Override
    public Optional<RenderedCard> renderCard(Card card) {
        return renderService.renderCard(card);
    }

    @Override
    public List<Revlog> getRevlogs() {
        return revlogRepository.getAllRevlogs();
    }

    @Override
    public Optional<Revlog> getRevlog(long id) {
        return revlogRepository.getRevlog(id);
    }

    @Override
    public List<Grave> getGraves() {
        return graveRepository.getAllGraves();
    }

    @Override
    public Optional<Grave> getGraveByOid(long oid) {
        return graveRepository.getGraveByOid(oid);
    }

    @Override
    public Optional<com.anki4j.model.Col> getCol() {
        return colRepository.getCol();
    }

    @Override
    public void save(Note note) {
        logger.info("Saving note ID: {}", note.getId());
        ankiWriter.save(note);
        this.dirty = true;
    }

    @Override
    public void addDeck(Deck deck) {
        logger.info("Adding deck: {}", deck.getName());
        deckRepository.addDeck(deck);
        this.dirty = true;
    }

    @Override
    public void addModel(Model model) {
        logger.info("Adding model: {}", model.getName());
        modelService.addModel(model);
        this.dirty = true;
    }

    @Override
    public void addNote(Note note) {
        logger.info("Adding note: {}", note.getId());
        noteRepository.addNote(note);
        this.dirty = true;
    }

    @Override
    public void addCard(Card card) {
        logger.info("Adding card: {}", card.getId());
        cardRepository.addCard(card);
        this.dirty = true;
    }

    @Override
    public void addMedia(String filename, byte[] content) {
        logger.info("Adding media: {}", filename);
        mediaManager.addMedia(filename, content);
        this.dirty = true;
    }

    // ==================== Resource Management ====================

    @Override
    public void close() {
        logger.info("Closing Anki4j session");

        if (dirty && originalPath != null) {
            try {
                byte[] updated = export();
                java.nio.file.Files.write(originalPath, updated);
                logger.info("Updated original APKG file: {}", originalPath);
            } catch (IOException e) {
                logger.error("Failed to persist changes to APKG file: {}", e.getMessage());
                throw new AnkiException("Failed to persist changes back to " + originalPath, e);
            }
        }

        try {
            if (connection != null && !connection.isClosed()) {
                logger.debug("Closing database connection");
                connection.close();
            }
        } catch (java.sql.SQLException e) {
            logger.error("Failed to close database connection: {}", e.getMessage());
        }
    }

    @Override
    public byte[] export() {
        logger.info("Exporting collection to APKG bytes");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 1. Save database to a temporary file and add to zip
            byte[] dbBytes = DatabaseManager.backup(connection);
            zos.putNextEntry(new ZipEntry("collection.anki21"));
            zos.write(dbBytes);
            zos.closeEntry();

            // 2. Add media
            Map<String, byte[]> mediaEntries = mediaManager.getAllMediaEntries();
            for (Map.Entry<String, byte[]> entry : mediaEntries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }

            // 3. Add 'media' JSON
            Map<String, String> filenameToZipName = mediaManager.getFilenameToZipName();
            if (!filenameToZipName.isEmpty()) {
                Map<String, String> reversedMediaMap = new java.util.HashMap<>();
                for (Map.Entry<String, String> entry : filenameToZipName.entrySet()) {
                    reversedMediaMap.put(entry.getValue(), entry.getKey());
                }
                byte[] mediaJson = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsBytes(reversedMediaMap);
                zos.putNextEntry(new ZipEntry("media"));
                zos.write(mediaJson);
                zos.closeEntry();
            }

        } catch (IOException e) {
            throw new AnkiException("Failed to export APKG", e);
        }
        return baos.toByteArray();
    }
}
