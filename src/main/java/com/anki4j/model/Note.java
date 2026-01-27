package com.anki4j.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)

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

    private transient com.anki4j.AnkiCollection context;

    @JsonIgnore
    public void setContext(com.anki4j.AnkiCollection context) {
        this.context = context;
    }

    @JsonIgnore
    public java.util.Optional<Model> getModel() {
        if (context == null) {
            throw new IllegalStateException("AnkiCollection context not set on this Note. Cannot lazy load model.");
        }
        return context.getModel(this.modelId);
    }

    private transient NoteFieldsMap fieldsMap;

    @JsonIgnore
    public NoteFieldsMap getFieldsMap() {
        if (fieldsMap == null) {
            Model model = getModel().orElseThrow(() -> new IllegalStateException("Model not found for this Note."));
            fieldsMap = new NoteFieldsMap(model, fields);
        }
        return fieldsMap;
    }

    /** Internal use for persistence */
    @JsonIgnore
    public void _setRawFields(String fields) {
        this.fields = fields;
        this.fieldsMap = null; // Invalidate map if raw string changes
    }

    @JsonIgnore
    public java.util.List<String> getMediaReferences() {
        java.util.List<String> media = new java.util.ArrayList<>();
        if (fields == null)
            return media;

        // Regex for [sound:filename]
        java.util.regex.Pattern soundPattern = java.util.regex.Pattern.compile("\\[sound:(.*?)\\]");
        java.util.regex.Matcher soundMatcher = soundPattern.matcher(fields);
        while (soundMatcher.find()) {
            media.add(soundMatcher.group(1));
        }

        // Regex for <img src="filename">
        java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>");
        java.util.regex.Matcher imgMatcher = imgPattern.matcher(fields);
        while (imgMatcher.find()) {
            media.add(imgMatcher.group(1));
        }

        return media;
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
