package com.anki4j;

import com.anki4j.model.Card;
import com.anki4j.model.Deck;
import com.anki4j.model.Note;

import java.util.List;
import java.util.Optional;

public interface AnkiCollection extends AutoCloseable {
    List<Deck> getDecks();

    Optional<Deck> getDeck(long deckId);

    List<Card> getCards(long deckId);

    Optional<Card> getCard(long cardId);

    Optional<Note> getNote(long noteId);

    Optional<Note> getNoteFromCard(long cardId);

    byte[] getMediaContent(String filename);
}
