package com.anki4j;

import com.anki4j.model.Card;
import com.anki4j.model.Deck;
import com.anki4j.model.Model;
import com.anki4j.model.Note;
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

    List<Deck> getDecks();

    List<Card> getCards(long deckId);

    Optional<Deck> getDeck(long deckId);

    Optional<Card> getCard(long cardId);

    Optional<Note> getNote(long noteId);

    Optional<Note> getNoteFromCard(long cardId);

    Optional<byte[]> getMediaContent(String filename);

    Optional<Model> getModel(long modelId);

    Optional<RenderedCard> renderCard(Card card);

    void save(Note note);

    void addDeck(Deck deck);

    void addModel(Model model);

    void addNote(Note note);

    void addCard(Card card);

    void addMedia(String filename, byte[] content);

    byte[] export();

    void close();
}
