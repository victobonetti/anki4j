package com.anki4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MediaManager {
    private static final Logger logger = LoggerFactory.getLogger(MediaManager.class);

    // Map from Filename (e.g. "image.jpg") to Zip Entry Name (e.g. "0")
    private final Map<String, String> filenameToZipName = new HashMap<>();
    private final ObjectMapper objectMapper;

    public MediaManager() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parses the 'media' JSON file from the zip to build the mapping.
     * The media file format is: {"0": "filename.jpg", "1": "audio.mp3"}
     */
    public void loadMap(ZipFile zipFile) throws IOException {
        ZipEntry mediaEntry = zipFile.getEntry("media");
        if (mediaEntry == null) {
            logger.warn("No 'media' file found in the archive.");
            return;
        }

        try (var inputStream = zipFile.getInputStream(mediaEntry)) {
            JsonNode root = objectMapper.readTree(inputStream);
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String zipName = field.getKey();
                String filename = field.getValue().asText();

                // Normalization could happen here if needed (e.g. NFC/NFD)
                // For now, we store as is.
                filenameToZipName.put(filename, zipName);
            }
        } catch (Exception e) {
            logger.error("Failed to parse media map: {}", e.getMessage());
            throw new IOException("Failed to parse media map", e);
        }
        logger.debug("Loaded {} media mappings.", filenameToZipName.size());
    }

    public String getZipEntryName(String filename) {
        return filenameToZipName.get(filename);
    }
}
