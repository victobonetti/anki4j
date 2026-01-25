package com.anki4j.model;

import java.util.*;

/**
 * Manages the fields of a Note, providing name-based access and tracking
 * modifications.
 */
public class NoteFieldsMap implements Iterable<NoteFieldsMap.FieldEntry> {

    public static class FieldEntry {
        private final String name;
        private String value;
        private boolean modified;

        public FieldEntry(String name, String value) {
            this.name = name;
            this.value = value;
            this.modified = false;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            if (!Objects.equals(this.value, value)) {
                this.value = value;
                this.modified = true;
            }
        }

        public boolean isModified() {
            return modified;
        }

        @Override
        public String toString() {
            return name + "=" + (value != null ? value : "");
        }

        void resetModified() {
            this.modified = false;
        }
    }

    private final List<FieldEntry> entries = new ArrayList<>();
    private final Map<String, FieldEntry> entryMap = new HashMap<>();
    private boolean dirty = false;

    public NoteFieldsMap(Model model, String rawFields) {
        String[] values = rawFields.split("\u001f", -1);
        List<Field> fieldDefs = model.getFlds();

        for (int i = 0; i < fieldDefs.size(); i++) {
            String name = fieldDefs.get(i).getName();
            String value = (i < values.length) ? values[i] : "";
            FieldEntry entry = new FieldEntry(name, value);
            entries.add(entry);
            entryMap.put(name, entry);
        }
    }

    public String get(String fieldName) {
        FieldEntry entry = entryMap.get(fieldName);
        return entry != null ? entry.getValue() : null;
    }

    public void set(String fieldName, String value) {
        FieldEntry entry = entryMap.get(fieldName);
        if (entry != null) {
            entry.setValue(value);
            if (entry.isModified()) {
                dirty = true;
            }
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    /**
     * Reconstructs the raw unit-separated string for persistence.
     */
    public String toRawString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0)
                sb.append("\u001f");
            sb.append(entries.get(i).getValue());
        }
        return sb.toString();
    }

    @Override
    public Iterator<FieldEntry> iterator() {
        return entries.iterator();
    }

    public void resetDirty() {
        dirty = false;
        for (FieldEntry entry : entries) {
            entry.resetModified();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(entries.get(i).toString());
        }
        sb.append("}");
        return sb.toString();
    }
}
