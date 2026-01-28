package com.anki4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Note implements Serializable {
    private long id;
    private String guid;
    private String fields; // Stored as separated string in Anki, typically unit-separated
    private long modelId;
    private boolean dirty = false;

    public Note() {
    }

    public Note(long id, String guid, String fields, long modelId) {
        this.id = id;
        this.guid = (guid == null || guid.isEmpty()) ? com.anki4j.internal.GuidGenerator.generate() : guid;
        this.fields = fields;
        this.modelId = modelId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getFields() {
        return fields;
    }

    public void setFields(String fields) {
        if (!java.util.Objects.equals(this.fields, fields)) {
            this.fields = fields;
            this.dirty = true;
        }
    }

    public long getModelId() {
        return modelId;
    }

    public void setModelId(long modelId) {
        this.modelId = modelId;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public String toString() {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            return super.toString();
        }
    }
}
