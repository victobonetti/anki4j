package com.anki4j;

import com.anki4j.exception.AnkiException;
import com.anki4j.internal.*;
import com.anki4j.model.Card;
import com.anki4j.model.Deck;
import com.anki4j.model.Model;
import com.anki4j.model.Note;
import com.anki4j.renderer.RenderedCard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class Anki4j implements AnkiCollection {

    private static final Logger logger = LoggerFactory.getLogger(Anki4j.class);

    // Resource handles
    private final Path tempDir;
    private final Connection connection;
    private final ZipFile zipFile;

    // Services
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final NoteRepository noteRepository;
    private final ModelService modelService;
    private final MediaManager mediaManager;
    private final RenderService renderService;

    private Anki4j(Path tempDir, Connection connection, ZipFile zipFile,
            DeckRepository deckRepository, CardRepository cardRepository,
            NoteRepository noteRepository, ModelService modelService,
            MediaManager mediaManager, RenderService renderService) {
        this.tempDir = tempDir;
        this.connection = connection;
        this.zipFile = zipFile;
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.noteRepository = noteRepository;
        this.modelService = modelService;
        this.mediaManager = mediaManager;
        this.renderService = renderService;
    }

    static Anki4j read(String path) {
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

            // Find database file
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

            // Initialize services
            MediaManager mediaManager = new MediaManager();
            mediaManager.loadMap(zip);

            CardRepository cardRepository = new CardRepository(conn);
            NoteRepository noteRepository = new NoteRepository(conn, cardRepository);
            DeckRepository deckRepository = new DeckRepository(conn);
            ModelService modelService = new ModelService(conn);
            RenderService renderService = new RenderService(noteRepository, modelService);

            Anki4j instance = new Anki4j(tempDir, conn, zip,
                    deckRepository, cardRepository, noteRepository,
                    modelService, mediaManager, renderService);

            // Set context for lazy loading
            deckRepository.setContext(instance);
            cardRepository.setContext(instance);

            return instance;

        } catch (Exception e) {
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
    public List<Card> getCards(long deckId) {
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
    public Optional<Note> getNoteFromCard(long cardId) {
        return noteRepository.getNoteFromCard(cardId);
    }

    @Override
    public Optional<byte[]> getMediaContent(String filename) {
        return mediaManager.getMediaContent(filename);
    }

    public Optional<Model> getModel(long modelId) {
        return modelService.getModel(modelId);
    }

    @Override
    public Optional<RenderedCard> renderCard(Card card) {
        return renderService.renderCard(card);
    }

    // ==================== Resource Management ====================

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
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
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
            e.printStackTrace();
        }
    }
}
