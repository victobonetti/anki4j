package com.anki4j.model;

import java.io.Serializable;

public class Note implements Serializable {
    private long id;
    private String guid;
    private String fields; // Stored as separated string in Anki, typically unit-separated
    private long modelId;

    public Note() {
    }

    public Note(long id, String guid, String fields, long modelId) {
        this.id = id;
        this.guid = guid;
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
        this.fields = fields;
    }

    public long getModelId() {
        return modelId;
    }

    public void setModelId(long modelId) {
        this.modelId = modelId;
    }

    public String[] getFieldParts() {
        if (fields == null)
            return new String[0];
        // Anki uses \u001f (Unit Separator) to separate fields
        return fields.split("\u001f");
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
