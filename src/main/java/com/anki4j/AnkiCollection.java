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

    void close();
}
