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

    @Override
    public String toString() {
        return "Card{id=" + id + ", noteId=" + noteId + ", deckId=" + deckId + ", ordinal=" + ordinal + "}";
    }
}
