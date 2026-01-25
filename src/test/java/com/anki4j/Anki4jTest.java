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
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement()) {

            // Create tables
            stmt.execute("CREATE TABLE decks (id INTEGER PRIMARY KEY, name TEXT)");
            stmt.execute("CREATE TABLE col (id INTEGER PRIMARY KEY, decks TEXT)"); // Fallback table
            stmt.execute(
                    "CREATE TABLE notes (id INTEGER PRIMARY KEY, guid TEXT, mid INTEGER, mod INTEGER, usn INTEGER, tags TEXT, flds TEXT, sfld INTEGER, csum INTEGER, flags INTEGER, data TEXT)");
            stmt.execute(
                    "CREATE TABLE cards (id INTEGER PRIMARY KEY, nid INTEGER, did INTEGER, ord INTEGER, mod INTEGER, usn INTEGER, type INTEGER, queue INTEGER, due INTEGER, ivl INTEGER, factor INTEGER, reps INTEGER, lapses INTEGER, left INTEGER, odue INTEGER, odid INTEGER, flags INTEGER, data TEXT)");

            // Insert data
            stmt.execute("INSERT INTO decks (id, name) VALUES (1, 'Default')");
            stmt.execute("INSERT INTO decks (id, name) VALUES (100, 'Test Deck')");

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
            stmt.execute("CREATE TABLE col (id INTEGER PRIMARY KEY, decks TEXT)");
            stmt.execute(
                    "CREATE TABLE notes (id INTEGER PRIMARY KEY, guid TEXT, mid INTEGER, mod INTEGER, usn INTEGER, tags TEXT, flds TEXT, sfld INTEGER, csum INTEGER, flags INTEGER, data TEXT)");
            stmt.execute(
                    "CREATE TABLE cards (id INTEGER PRIMARY KEY, nid INTEGER, did INTEGER, ord INTEGER, mod INTEGER, usn INTEGER, type INTEGER, queue INTEGER, due INTEGER, ivl INTEGER, factor INTEGER, reps INTEGER, lapses INTEGER, left INTEGER, odue INTEGER, odid INTEGER, flags INTEGER, data TEXT)");

            // Insert data with JSON in 'col.decks'
            String decksJson = "{\"1\": {\"name\": \"Default\", \"id\": 1}, \"200\": {\"name\": \"Legacy Deck\", \"id\": 200}}";
            stmt.execute("INSERT INTO col (id, decks) VALUES (1, '" + decksJson + "')");
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

            // Verify Notes
            List<Note> notes = anki.getNotes();
            assertEquals(1, notes.size());
            assertEquals("Front\u001fBack", notes.get(0).getFields());
            assertEquals("Front", notes.get(0).getTitle());
            assertEquals("Back", notes.get(0).getContent());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Should not throw exception: " + e.getMessage());
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
}
