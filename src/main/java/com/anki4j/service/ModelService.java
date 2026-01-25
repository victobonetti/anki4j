package com.anki4j.service;

import com.anki4j.exception.AnkiException;
import com.anki4j.model.Model;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class ModelService {

    private final Map<Long, Model> modelCache = new HashMap<>();
    private final Connection connection;
    private final ObjectMapper objectMapper;

    public ModelService(Connection connection) {
        this.connection = connection;
        this.objectMapper = new ObjectMapper();
        loadModels();
    }

    private void loadModels() {
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
                            // Key is model ID
                            long id = Long.parseLong(field.getKey());
                            Model model = objectMapper.treeToValue(field.getValue(), Model.class);
                            // Ensure ID is set if not present in value (though usually it is)
                            if (model.getId() == 0) {
                                model.setId(id);
                            }
                            modelCache.put(id, model);
                        }
                    } catch (JsonProcessingException e) {
                        throw new AnkiException("Failed to parse models JSON", e);
                    }
                }
            }
        } catch (SQLException e) {
            throw new AnkiException("Failed to load models from database", e);
        }
    }

    public Optional<Model> getModel(long modelId) {
        return Optional.ofNullable(modelCache.get(modelId));
    }
}
