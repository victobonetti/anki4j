package com.anki4j;

import com.anki4j.model.Card;
import com.anki4j.model.Deck;
import com.anki4j.model.Note;

import java.util.List;

public interface AnkiCollection extends AutoCloseable {
    List<Deck> getDecks();

    List<Card> getCards(long deckId);

    Note getNote(long noteId);

    byte[] getMediaContent(String filename);
}
