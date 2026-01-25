package com.anki4j.model;

import java.io.Serializable;

public class Card implements Serializable {
    private long id;
    private long noteId;
    private long deckId;
    private long ordinal;

    public Card() {
    }

    public Card(long id, long noteId, long deckId, long ordinal) {
        this.id = id;
        this.noteId = noteId;
        this.deckId = deckId;
        this.ordinal = ordinal;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getNoteId() {
        return noteId;
    }

    public void setNoteId(long noteId) {
        this.noteId = noteId;
    }

    public long getDeckId() {
        return deckId;
    }

    public void setDeckId(long deckId) {
        this.deckId = deckId;
    }

    public long getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(long ordinal) {
        this.ordinal = ordinal;
    }

    private transient com.anki4j.AnkiCollection context;

    public void setContext(com.anki4j.AnkiCollection context) {
        this.context = context;
    }

    public Note getNote() {
        if (context == null) {
            throw new IllegalStateException("AnkiCollection context not set on this Card. Cannot lazy load note.");
        }
        return context.getNote(this.noteId);
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
