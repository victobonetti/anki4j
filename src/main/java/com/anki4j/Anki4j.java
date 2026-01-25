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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class Anki4j implements AnkiCollection {

    private static final Logger logger = LoggerFactory.getLogger(Anki4j.class);

    // Resource handles
    private final Path originalPath;
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
    private final AnkiWriter ankiWriter;

    private boolean dirty = false;

    private Anki4j(Path originalPath, Path tempDir, Connection connection, ZipFile zipFile,
            DeckRepository deckRepository, CardRepository cardRepository,
            NoteRepository noteRepository, ModelService modelService,
            MediaManager mediaManager, RenderService renderService,
            AnkiWriter ankiWriter) {
        this.originalPath = originalPath;
        this.tempDir = tempDir;
        this.connection = connection;
        this.zipFile = zipFile;
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.noteRepository = noteRepository;
        this.modelService = modelService;
        this.mediaManager = mediaManager;
        this.renderService = renderService;
        this.ankiWriter = ankiWriter;
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
            AnkiWriter ankiWriter = new AnkiWriter(conn);

            Anki4j instance = new Anki4j(apkgPath, tempDir, conn, zip,
                    deckRepository, cardRepository, noteRepository,
                    modelService, mediaManager, renderService, ankiWriter);

            // Set context for lazy loading
            deckRepository.setContext(instance);
            cardRepository.setContext(instance);
            noteRepository.setContext(instance);

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

    @Override
    public Optional<Model> getModel(long modelId) {
        return modelService.getModel(modelId);
    }

    @Override
    public Optional<RenderedCard> renderCard(Card card) {
        return renderService.renderCard(card);
    }

    @Override
    public void save(Note note) {
        ankiWriter.save(note);
        this.dirty = true;
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

        if (dirty) {
            try {
                persistChanges();
            } catch (IOException e) {
                logger.error("Failed to persist changes to APKG file: {}", e.getMessage());
                throw new AnkiException("Failed to persist changes back to " + originalPath, e);
            }
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

    private void persistChanges() throws IOException {
        Path tempApkg = tempDir.resolve("updated.apkg");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempApkg.toFile()))) {
            // 1. Write the current database file(s) from tempDir
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
                for (Path entry : stream) {
                    if (Files.isRegularFile(entry) && entry.getFileName().toString().startsWith("collection.anki2")) {
                        addToZip(entry, entry.getFileName().toString(), zos);
                    }
                }
            }

            // 2. Copy everything else from the original zip
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                // Skip the database files we already wrote
                if (name.equals("collection.anki2") || name.equals("collection.anki21")) {
                    continue;
                }

                zos.putNextEntry(new ZipEntry(name));
                try (InputStream is = zipFile.getInputStream(entry)) {
                    is.transferTo(zos);
                }
                zos.closeEntry();
            }
        }

        // Replace original file with temporary updated one
        Files.move(tempApkg, originalPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Successfully updated APKG file: {}", originalPath);
    }

    private void addToZip(Path file, String name, ZipOutputStream zos) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        Files.copy(file, zos);
        zos.closeEntry();
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
