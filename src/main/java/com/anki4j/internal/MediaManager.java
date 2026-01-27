package com.anki4j.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MediaManager {
    private static final Logger logger = LoggerFactory.getLogger(MediaManager.class);

    private final Map<String, String> filenameToZipName = new HashMap<>();
    private final ObjectMapper objectMapper;
    private ZipFile zipFile;

    public MediaManager() {
        logger.info("Initializing MediaManager");
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parses the 'media' JSON file from the zip to build the mapping.
     * The media file format is: {"0": "filename.jpg", "1": "audio.mp3"}
     */
    public void loadMap(ZipFile zipFile) throws IOException {
        logger.info("Loading media map from zip file");
        this.zipFile = zipFile;
        ZipEntry mediaEntry = zipFile.getEntry("media");
        if (mediaEntry == null) {
            logger.warn("No 'media' file found in the archive.");
            return;
        }

        try (InputStream inputStream = zipFile.getInputStream(mediaEntry)) {
            JsonNode root = objectMapper.readTree(inputStream);
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

        ZipEntry entry = zipFile.getEntry(zipName);
        if (entry == null) {
            logger.info("Zip entry '{}' not found for media '{}'", zipName, filename);
            return Optional.empty();
        }

        try (InputStream is = zipFile.getInputStream(entry)) {
            byte[] data = is.readAllBytes();
            logger.info("Successfully read {} bytes for media '{}'", data.length, filename);
            return Optional.of(data);
        } catch (IOException e) {
            logger.error("Failed to read media file '{}' (zip entry '{}'): {}", filename, zipName, e.getMessage());
            return Optional.empty();
        }
    }
}
