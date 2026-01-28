package com.anki4j;

import com.anki4j.model.*;
import com.anki4j.renderer.RenderedCard;

import java.util.List;
import java.util.Optional;

public sealed interface AnkiCollection extends AutoCloseable permits Anki4j {

    static AnkiCollection read(String path) {
        return Anki4j.read(path);
    }

    static AnkiCollection read(java.io.InputStream inputStream) {
        return Anki4j.read(inputStream);
    }

    static AnkiCollection read(byte[] data) {
        return Anki4j.read(data);
    }

    // --- Entity Getters ---

    List<Deck> getDecks();

    Optional<Deck> getDeck(long deckId);

    List<Card> getCards();

    List<Card> getCards(long deckId);

    Optional<Card> getCard(long cardId);

    List<Note> getNotes();

    Optional<Note> getNote(long noteId);

    Optional<Note> getNoteFromCard(long cardId);

    List<Model> getModels();

    Optional<Model> getModel(long modelId);

    List<Revlog> getRevlogs();

    Optional<Revlog> getRevlog(long id);

    List<Grave> getGraves();

    Optional<Grave> getGraveByOid(long oid);

    Optional<Col> getCol();

    // --- Operations ---

    void save(Note note);

    Optional<byte[]> getMediaContent(String filename);

    Optional<RenderedCard> renderCard(Card card);

    void addDeck(Deck deck);

    void addModel(Model model);

    void addNote(Note note);

    void addCard(Card card);

    void addMedia(String filename, byte[] content);

    byte[] export();

    void close();
}
