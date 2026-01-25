package com.anki4j;

import com.anki4j.model.Card;
import com.anki4j.model.Deck;
import com.anki4j.model.Note;

import java.util.List;

public interface AnkiCollection extends AutoCloseable {
    List<Deck> getDecks();

    Deck getDeck(long deckId);

    List<Card> getCards(long deckId);

    Card getCard(long cardId);

    Note getNote(long noteId);

    Note getNoteFromCard(long cardId);

    byte[] getMediaContent(String filename);
}
