package com.anki4j.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)

public class Deck implements Serializable {
    private long id;
    private String name;

    public Deck() {
    }

    public Deck(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private transient com.anki4j.AnkiCollection context;

    @JsonIgnore
    public void setContext(com.anki4j.AnkiCollection context) {
        this.context = context;
    }

    @JsonIgnore
    public java.util.List<Card> getCards() {
        if (context == null) {
            throw new IllegalStateException("AnkiCollection context not set on this Deck. Cannot lazy load cards.");
        }
        return context.getCards(this.id);
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
