package com.anki4j;

import com.anki4j.model.Card;
import com.anki4j.model.Deck;
import com.anki4j.model.Note;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class Anki4jTest {

    private Path tempTestDir;
    private Path apkgPath;

    @Before
    public void setUp() throws Exception {
        tempTestDir = Files.createTempDirectory("anki4j_test_gen");
        createSyntheticApkg();
    }

    @After
    public void tearDown() throws IOException {
        if (tempTestDir != null) {
            Files.walkFileTree(tempTestDir, new SimpleFileVisitor<Path>() {
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
        }
    }

    private void createSyntheticApkg() throws Exception {
        // 1. Create a temporary SQLite DB
        Path dbPath = tempTestDir.resolve("collection.anki2");
        Files.deleteIfExists(dbPath);
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement()) {

            // Create tables
            stmt.execute("CREATE TABLE decks (id INTEGER PRIMARY KEY, name TEXT)");
            stmt.execute("CREATE TABLE col (id INTEGER PRIMARY KEY, decks TEXT, models TEXT)"); // Fallback table
            stmt.execute(
                    "CREATE TABLE notes (id INTEGER PRIMARY KEY, guid TEXT, mid INTEGER, mod INTEGER, usn INTEGER, tags TEXT, flds TEXT, sfld INTEGER, csum INTEGER, flags INTEGER, data TEXT)");
            stmt.execute(
                    "CREATE TABLE cards (id INTEGER PRIMARY KEY, nid INTEGER, did INTEGER, ord INTEGER, mod INTEGER, usn INTEGER, type INTEGER, queue INTEGER, due INTEGER, ivl INTEGER, factor INTEGER, reps INTEGER, lapses INTEGER, left INTEGER, odue INTEGER, odid INTEGER, flags INTEGER, data TEXT)");

            // Insert data
            stmt.execute("INSERT INTO decks (id, name) VALUES (1, 'Default')");
            stmt.execute("INSERT INTO decks (id, name) VALUES (100, 'Test Deck')");

            // Model with 2 fields: Front, Back
            String modelsJson = "{\"1\": {\"id\": 1, \"name\": \"Basic\", \"flds\": [{\"name\": \"Front\", \"ord\": 0}, {\"name\": \"Back\", \"ord\": 1}], \"tmpls\": [{\"name\": \"Card 1\", \"qfmt\": \"{{Front}}\", \"afmt\": \"{{FrontSide}}<hr id=answer>{{Back}}\"}]}}";
            stmt.execute("INSERT INTO col (models) VALUES ('" + modelsJson + "')");

            stmt.execute("INSERT INTO notes (id, guid, flds, mid) VALUES (10, 'guid1', 'Front\u001fBack', 1)");
            stmt.execute("INSERT INTO cards (id, nid, did, ord) VALUES (1000, 10, 100, 0)");
        }

        // 2. Zip it into .apkg
        apkgPath = tempTestDir.resolve("test.apkg");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(apkgPath.toFile()))) {
            ZipEntry entry = new ZipEntry("collection.anki2");
            zos.putNextEntry(entry);
            Files.copy(dbPath, zos);
            zos.closeEntry();
        }
    }

    private void createLegacyApkg() throws Exception {
        // 1. Create a temporary SQLite DB (LEGACY: No 'decks' table, data in 'col')
        Path dbPath = tempTestDir.resolve("collection.anki2");
        Files.deleteIfExists(dbPath); // Delete the one created by setUp()

        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement()) {

            // Create tables
            stmt.execute("CREATE TABLE col (id INTEGER PRIMARY KEY, decks TEXT, models TEXT)");
            stmt.execute(
                    "CREATE TABLE notes (id INTEGER PRIMARY KEY, guid TEXT, mid INTEGER, mod INTEGER, usn INTEGER, tags TEXT, flds TEXT, sfld INTEGER, csum INTEGER, flags INTEGER, data TEXT)");
            stmt.execute(
                    "CREATE TABLE cards (id INTEGER PRIMARY KEY, nid INTEGER, did INTEGER, ord INTEGER, mod INTEGER, usn INTEGER, type INTEGER, queue INTEGER, due INTEGER, ivl INTEGER, factor INTEGER, reps INTEGER, lapses INTEGER, left INTEGER, odue INTEGER, odid INTEGER, flags INTEGER, data TEXT)");

            // Insert data with JSON in 'col.decks'
            String decksJson = "{\"1\": {\"name\": \"Default\", \"id\": 1}, \"200\": {\"name\": \"Legacy Deck\", \"id\": 200}}";
            stmt.execute("INSERT INTO col (id, decks, models) VALUES (1, '" + decksJson + "', '{}')");
        }

        // 2. Zip it into .apkg
        apkgPath = tempTestDir.resolve("legacy.apkg");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(apkgPath.toFile()))) {
            ZipEntry entry = new ZipEntry("collection.anki2");
            zos.putNextEntry(entry);
            Files.copy(dbPath, zos);
            zos.closeEntry();
        }
    }

    @Test
    public void testLazyLoading() {
        try (Anki4j anki = Anki4j.read(apkgPath.toString())) {
            List<Deck> decks = anki.getDecks();
            assertFalse(decks.isEmpty());

            Deck testDeck = decks.stream()
                    .filter(d -> d.getId() == 100)
                    .findFirst()
                    .orElse(null);
            assertNotNull(testDeck);

            // Lazy load cards
            List<Card> cards = testDeck.getCards();
            assertEquals(1, cards.size());
            Card card = cards.get(0);

            // Verify Note retrieval via Card
            java.util.Optional<Note> noteOpt = card.getNote();
            assertTrue(noteOpt.isPresent());
            Note note = noteOpt.get();
            assertEquals("Front\u001fBack", note.getFields());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testCollectionMethods() {
        try (AnkiCollection anki = Anki4j.read(apkgPath.toString())) {
            // Test getDeck
            java.util.Optional<Deck> deck = anki.getDeck(100);
            assertTrue(deck.isPresent());
            assertEquals("Test Deck", deck.get().getName());

            // Test getCard
            java.util.Optional<Card> card = anki.getCard(1000);
            assertTrue(card.isPresent());
            assertEquals(100, card.get().getDeckId());
            assertEquals(10, card.get().getNoteId());

            // Test getNoteFromCard
            java.util.Optional<Note> note = anki.getNoteFromCard(1000);
            assertTrue(note.isPresent());
            assertEquals(10, note.get().getId());

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void createMediaApkg() throws Exception {
        // 1. Create a temporary SQLite DB (LEGACY: No 'decks' table, data in 'col')
        Path dbPath = tempTestDir.resolve("collection.anki2");
        Files.deleteIfExists(dbPath);

        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement()) {

            // Create tables
            stmt.execute("CREATE TABLE decks (id INTEGER PRIMARY KEY, name TEXT)");
            stmt.execute("CREATE TABLE col (id INTEGER PRIMARY KEY, decks TEXT, models TEXT)");
            stmt.execute(
                    "CREATE TABLE notes (id INTEGER PRIMARY KEY, guid TEXT, mid INTEGER, mod INTEGER, usn INTEGER, tags TEXT, flds TEXT, sfld INTEGER, csum INTEGER, flags INTEGER, data TEXT)");
            stmt.execute(
                    "CREATE TABLE cards (id INTEGER PRIMARY KEY, nid INTEGER, did INTEGER, ord INTEGER, mod INTEGER, usn INTEGER, type INTEGER, queue INTEGER, due INTEGER, ivl INTEGER, factor INTEGER, reps INTEGER, lapses INTEGER, left INTEGER, odue INTEGER, odid INTEGER, flags INTEGER, data TEXT)");

            stmt.execute("INSERT INTO decks (id, name) VALUES (1, 'Default')");
            stmt.execute("INSERT INTO col (models) VALUES ('{}')");
            // Insert note with media refs using char 31 as separator
            // String flds = "Bird<img src=\"bird.jpg\">" + (char) 31 + "[sound:chirp.mp3]";
            // stmt.execute("INSERT INTO notes (id, guid, flds, mid) VALUES (500,
            // 'guidMedia', ?, 1)");

            // Need to use PreparedStatement for special chars if direct insert fails, but
            // string concat should be fine here for basic test
            // Actually, let's use prepared statement for safety
        }
        try (Connection conn = DriverManager.getConnection(url);
                java.sql.PreparedStatement pstmt = conn
                        .prepareStatement("INSERT INTO notes (id, guid, flds, mid) VALUES (500, 'guidMedia', ?, 1)")) {
            pstmt.setString(1, "Bird<img src=\"bird.jpg\">" + (char) 31 + "[sound:chirp.mp3]");
            pstmt.executeUpdate();
        }

        // 2. Zip it into .apkg with media file and dummy assets
        apkgPath = tempTestDir.resolve("media.apkg");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(apkgPath.toFile()))) {
            // DB
            ZipEntry entry = new ZipEntry("collection.anki2");
            zos.putNextEntry(entry);
            Files.copy(dbPath, zos);
            zos.closeEntry();

            // Media Map
            ZipEntry media = new ZipEntry("media");
            zos.putNextEntry(media);
            // "0": "bird.jpg", "1": "chirp.mp3"
            String json = "{\"0\": \"bird.jpg\", \"1\": \"chirp.mp3\"}";
            zos.write(json.getBytes());
            zos.closeEntry();

            // Assets
            ZipEntry asset0 = new ZipEntry("0");
            zos.putNextEntry(asset0);
            zos.write("IMAGE_DATA".getBytes());
            zos.closeEntry();

            ZipEntry asset1 = new ZipEntry("1");
            zos.putNextEntry(asset1);
            zos.write("AUDIO_DATA".getBytes());
            zos.closeEntry();
        }
    }

    @Test
    public void testMediaSupport() throws Exception {
        createMediaApkg();
        try (Anki4j anki = Anki4j.read(apkgPath.toString())) {

            // Check Note References
            java.util.Optional<Note> noteOpt = anki.getNote(500);
            assertTrue(noteOpt.isPresent());
            Note note = noteOpt.get();

            List<String> mediaRefs = note.getMediaReferences();
            assertEquals(2, mediaRefs.size());
            assertTrue(mediaRefs.contains("bird.jpg"));
            assertTrue(mediaRefs.contains("chirp.mp3"));

            // Check Content Extraction
            Optional<byte[]> imageBytes = anki.getMediaContent("bird.jpg");
            assertTrue(imageBytes.isPresent());
            assertEquals("IMAGE_DATA", new String(imageBytes.get()));

            Optional<byte[]> audioBytes = anki.getMediaContent("chirp.mp3");
            assertTrue(audioBytes.isPresent());
            assertEquals("AUDIO_DATA", new String(audioBytes.get()));

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testReadLegacyDecks() throws Exception {
        createLegacyApkg();
        try (Anki4j anki = Anki4j.read(apkgPath.toString())) {
            // Verify Decks from JSON
            List<Deck> decks = anki.getDecks();
            assertEquals(2, decks.size());
            boolean found = decks.stream().anyMatch(d -> d.getName().equals("Legacy Deck") && d.getId() == 200);
            assertTrue("Should contain 'Legacy Deck' parsed from JSON", found);
            assertTrue("Should contain 'Default'", decks.stream().anyMatch(d -> d.getName().equals("Default")));
        }
    }

    @Test
    public void testPersistence() throws Exception {
        // Core persistence test trace
        // 1. Setup
        createSyntheticApkg();
        String originalPathString = apkgPath.toString();

        // 2. Edit and Save
        try (AnkiCollection anki = Anki4j.read(originalPathString)) {
            Note note = anki.getNote(10).get();
            note.getFieldsMap().set("Front", "Updated Front");
            note.getFieldsMap().set("Back", "Updated Back");
            anki.save(note);
        } // Triggers re-zipping

        // 3. Verify Persistence by re-opening
        try (AnkiCollection anki = Anki4j.read(originalPathString)) {
            Note note = anki.getNote(10).get();
            assertEquals("Updated Front", note.getFieldsMap().get("Front"));
            assertEquals("Updated Back", note.getFieldsMap().get("Back"));
            assertEquals("Updated Front\u001fUpdated Back", note.getFields());
        }
    }

    @Test
    public void testReadFromInputStream() throws Exception {
        try (java.io.InputStream is = Files.newInputStream(apkgPath);
                AnkiCollection anki = AnkiCollection.read(is)) {
            List<Deck> decks = anki.getDecks();
            assertFalse("Decks should not be empty when reading from InputStream", decks.isEmpty());
            assertTrue("Should contain 'Test Deck'", decks.stream().anyMatch(d -> d.getName().equals("Test Deck")));

            // Verify content
            Optional<Card> card = anki.getCard(1000);
            assertTrue(card.isPresent());
        }
    }

    @Test
    public void testReadFromByteArray() throws Exception {
        byte[] data = Files.readAllBytes(apkgPath);
        try (AnkiCollection anki = AnkiCollection.read(data)) {
            List<Deck> decks = anki.getDecks();
            assertFalse("Decks should not be empty when reading from byte array", decks.isEmpty());
            assertTrue("Should contain 'Test Deck'", decks.stream().anyMatch(d -> d.getName().equals("Test Deck")));

            // Verify content
            Optional<Card> card = anki.getCard(1000);
            assertTrue(card.isPresent());
        }
    }
}
