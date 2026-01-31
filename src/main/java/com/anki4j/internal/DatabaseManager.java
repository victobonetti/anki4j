package com.anki4j.internal;

import com.anki4j.exception.AnkiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    public static void initializeSchema(Connection conn) throws SQLException {
        logger.info("Initializing new Anki database schema");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS col (id INTEGER PRIMARY KEY, crt INTEGER NOT NULL, mod INTEGER NOT NULL, scm INTEGER NOT NULL, ver INTEGER NOT NULL, dty INTEGER NOT NULL, usn INTEGER NOT NULL, ls INTEGER NOT NULL, conf TEXT NOT NULL, models TEXT NOT NULL, decks TEXT NOT NULL, dconf TEXT NOT NULL, tags TEXT NOT NULL)");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY, guid TEXT NOT NULL, mid INTEGER NOT NULL, mod INTEGER NOT NULL, usn INTEGER NOT NULL, tags TEXT NOT NULL, flds TEXT NOT NULL, sfld TEXT NOT NULL, csum INTEGER NOT NULL, flags INTEGER NOT NULL, data TEXT NOT NULL)");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS cards (id INTEGER PRIMARY KEY, nid INTEGER NOT NULL, did INTEGER NOT NULL, ord INTEGER NOT NULL, mod INTEGER NOT NULL, usn INTEGER NOT NULL, type INTEGER NOT NULL, queue INTEGER NOT NULL, due INTEGER NOT NULL, ivl INTEGER NOT NULL, factor INTEGER NOT NULL, reps INTEGER NOT NULL, lapses INTEGER NOT NULL, left INTEGER NOT NULL, odue INTEGER NOT NULL, odid INTEGER NOT NULL, flags INTEGER NOT NULL, data TEXT NOT NULL)");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS revlog (id INTEGER PRIMARY KEY, cid INTEGER NOT NULL, usn INTEGER NOT NULL, ease INTEGER NOT NULL, ivl INTEGER NOT NULL, lastIvl INTEGER NOT NULL, factor INTEGER NOT NULL, time INTEGER NOT NULL, type INTEGER NOT NULL)");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS graves (usn INTEGER NOT NULL, oid INTEGER NOT NULL, type INTEGER NOT NULL)");

            // Initialize 'col' table with default structure
            long now = System.currentTimeMillis() / 1000;
            stmt.execute("INSERT INTO col (id, crt, mod, scm, ver, dty, usn, ls, conf, models, decks, dconf, tags) " +
                    "VALUES (1, " + now + ", " + now + ", " + now + ", 11, 0, 0, 0, '{}', '{}', '{}', '{}', '{}')");
        }
    }

    public static byte[] backup(Connection conn) {
        java.nio.file.Path tempFile = null;
        try {
            tempFile = java.nio.file.Files.createTempFile("anki4j_export", ".db");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("backup to " + tempFile.toAbsolutePath());
            }
            return java.nio.file.Files.readAllBytes(tempFile);
        } catch (Exception e) {
            throw new AnkiException("Failed to backup memory database", e);
        } finally {
            if (tempFile != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void restore(Connection conn, byte[] dbBytes) throws Exception {
        logger.info("Restoring database bytes into in-memory SQLite");
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("anki4j_db", ".db");
        try {
            java.nio.file.Files.write(tempFile, dbBytes);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("restore from " + tempFile.toAbsolutePath());
            }
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    public static byte[] extractDatabaseBytes(byte[] zipData) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            byte[] result21 = null;
            byte[] result20 = null;

            try {
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("collection.anki21")) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        zis.transferTo(baos);
                        result21 = baos.toByteArray();
                        break;
                    } else if (entry.getName().equals("collection.anki2")) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        zis.transferTo(baos);
                        result20 = baos.toByteArray();
                    }
                    zis.closeEntry();
                }
            } catch (java.util.zip.ZipException e) {
                throw new AnkiException("Invalid APKG file format: Not a valid zip archive", e);
            }
            return result21 != null ? result21 : result20;
        }
    }
}
