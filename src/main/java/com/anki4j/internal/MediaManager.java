package com.anki4j.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class MediaManager {
    private static final Logger logger = LoggerFactory.getLogger(MediaManager.class);

    private final Map<String, String> filenameToZipName = new HashMap<>();
    private final Map<String, byte[]> zipEntryBytes = new HashMap<>();
    private final ObjectMapper objectMapper;

    public MediaManager() {
        logger.info("Initializing MediaManager");
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parses the 'media' JSON file and loads all media content into memory.
     */
    public void load(byte[] zipData) throws IOException {
        logger.info("Loading media map and content from zip data");

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            byte[] mediaJsonBytes = null;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                zis.transferTo(baos);
                byte[] content = baos.toByteArray();

                if (name.equals("media")) {
                    mediaJsonBytes = content;
                } else {
                    zipEntryBytes.put(name, content);
                }
                zis.closeEntry();
            }

            if (mediaJsonBytes == null) {
                logger.warn("No 'media' file found in the archive.");
                return;
            }

            JsonNode root = objectMapper.readTree(mediaJsonBytes);
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String zipName = field.getKey();
                String filename = field.getValue().asText();
                filenameToZipName.put(filename, zipName);
            }
        } catch (Exception e) {
            logger.error("Failed to parse media map: {}", e.getMessage());
            throw new IOException("Failed to parse media map", e);
        }
        logger.info("Loaded {} media mappings.", filenameToZipName.size());
    }

    public String getZipEntryName(String filename) {
        return filenameToZipName.get(filename);
    }

    /**
     * Gets media content as bytes.
     * 
     * @param filename The media filename
     * @return Optional containing bytes, or empty if not found
     */
    public Optional<byte[]> getMediaContent(String filename) {
        logger.info("Retrieving media content: {}", filename);
        String zipName = filenameToZipName.get(filename);
        if (zipName == null) {
            logger.info("Media mapping not found for: {}", filename);
            return Optional.empty();
        }

        byte[] content = zipEntryBytes.get(zipName);
        if (content == null) {
            logger.info("Zip entry content '{}' not found for media '{}'", zipName, filename);
            return Optional.empty();
        }

        logger.info("Successfully retrieved {} bytes for media '{}'", content.length, filename);
        return Optional.of(content);
    }

    public Map<String, byte[]> getAllMediaEntries() {
        return Collections.unmodifiableMap(zipEntryBytes);
    }

    public Map<String, String> getFilenameToZipName() {
        return Collections.unmodifiableMap(filenameToZipName);
    }

    public void addMedia(String filename, byte[] content) {
        logger.info("Adding new media: {}", filename);
        // Find a unique zip name (e.g., using next available integer)
        int maxZipName = -1;
        for (String zipName : filenameToZipName.values()) {
            try {
                int val = Integer.parseInt(zipName);
                if (val > maxZipName) {
                    maxZipName = val;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        String nextZipName = String.valueOf(maxZipName + 1);
        filenameToZipName.put(filename, nextZipName);
        zipEntryBytes.put(nextZipName, content);
    }
}
