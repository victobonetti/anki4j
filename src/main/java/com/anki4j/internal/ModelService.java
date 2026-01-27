package com.anki4j.internal;

import com.anki4j.exception.AnkiException;
import com.anki4j.model.Model;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class ModelService {
    private static final Logger logger = LoggerFactory.getLogger(ModelService.class);

    private final Map<Long, Model> modelCache = new HashMap<>();
    private final ObjectMapper objectMapper;

    public ModelService(Connection connection) {
        logger.info("Initializing ModelService");
        this.objectMapper = new ObjectMapper();
        loadModels(connection);
    }

    private void loadModels(Connection connection) {
        logger.info("Loading models from database");
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT models FROM col LIMIT 1")) {

            if (rs.next()) {
                String json = rs.getString("models");
                if (json != null && !json.isEmpty()) {
                    try {
                        JsonNode root = objectMapper.readTree(json);
                        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> field = fields.next();
                            long id = Long.parseLong(field.getKey());
                            Model model = objectMapper.treeToValue(field.getValue(), Model.class);
                            if (model.getId() == 0) {
                                model.setId(id);
                            }
                            modelCache.put(id, model);
                        }
                    } catch (JsonProcessingException e) {
                        logger.error("Failed to parse models JSON: {}", e.getMessage());
                        throw new AnkiException("Failed to parse models JSON", e);
                    }
                }
            }
            logger.info("Loaded {} models into cache", modelCache.size());
        } catch (SQLException e) {
            logger.error("Failed to load models from database: {}", e.getMessage());
            throw new AnkiException("Failed to load models from database", e);
        }
    }

    public Optional<Model> getModel(long modelId) {
        logger.info("Fetching model with ID: {}", modelId);
        Model model = modelCache.get(modelId);
        if (model != null) {
            logger.info("Model found in cache: {}", modelId);
        } else {
            logger.info("Model not found in cache: {}", modelId);
        }
        return Optional.ofNullable(model);
    }
}
