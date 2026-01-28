package com.anki4j;

import com.anki4j.model.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class AnkiCreationTest {

    @Test
    public void testCreateEmptyCollectionAndExport() throws IOException {
        try (AnkiCollection anki = Anki4j.create()) {
            // 1. Add Model
            Model model = new Model();
            model.setId(123456L);
            model.setName("Basic Test Model");

            Field front = new Field();
            front.setName("Front");
            front.setOrd(0);

            Field back = new Field();
            back.setName("Back");
            back.setOrd(1);

            model.setFlds(List.of(front, back));

            Template template = new Template();
            template.setName("Card 1");
            template.setQfmt("{{Front}}");
            template.setAfmt("{{FrontSide}}<hr id=answer>{{Back}}");

            model.setTmpls(List.of(template));

            anki.addModel(model);

            // 2. Add Deck
            Deck deck = new Deck(1L, "Default");
            anki.addDeck(deck);

            Deck customDeck = new Deck(100L, "Custom Deck");
            anki.addDeck(customDeck);

            // 3. Add Note (testing GUID auto-generation by passing null)
            Note note = new Note(10L, null, "Front content" + (char) 31 + "Back content", 123456L);
            String generatedGuid = note.getGuid();
            assertNotNull(generatedGuid);
            assertEquals(10, generatedGuid.length());
            anki.addNote(note);

            // 4. Add Card
            Card card = new Card(1000L, 10L, 100L, 0);
            anki.addCard(card);

            // 5. Add Media
            byte[] dummyImage = "DUMMY_IMAGE_DATA".getBytes();
            anki.addMedia("test_image.jpg", dummyImage);

            // 6. Export to APKG
            byte[] apkgBytes = anki.export();
            assertNotNull(apkgBytes);
            assertTrue(apkgBytes.length > 0);

            // 7. Verify by re-reading
            try (AnkiCollection reRead = Anki4j.read(apkgBytes)) {
                // Verify Deck
                Optional<Deck> savedDeck = reRead.getDeck(100L);
                assertTrue(savedDeck.isPresent());
                assertEquals("Custom Deck", savedDeck.get().getName());

                // Verify Model
                Optional<Model> savedModel = reRead.getModel(123456L);
                assertTrue(savedModel.isPresent());
                assertEquals("Basic Test Model", savedModel.get().getName());

                // Verify Note
                Optional<Note> savedNote = reRead.getNote(10L);
                assertTrue(savedNote.isPresent());
                assertEquals("Front content\u001fBack content", savedNote.get().getFlds());

                // Verify Card
                Optional<Card> savedCard = reRead.getCard(1000L);
                assertTrue(savedCard.isPresent());
                assertEquals(100L, savedCard.get().getDid());

                // Verify Media
                Optional<byte[]> savedMedia = reRead.getMediaContent("test_image.jpg");
                assertTrue(savedMedia.isPresent());
                assertArrayEquals(dummyImage, savedMedia.get());
            }
        }
    }
}
