package com.anki4j.renderer;

import com.anki4j.model.Field;
import com.anki4j.model.Model;
import com.anki4j.model.Note;
import com.anki4j.model.Template;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RendererTest {

    private Renderer renderer;
    private Model model;
    private Note note;

    @Before
    public void setUp() {
        renderer = new Renderer();

        // Setup Model
        model = new Model();
        model.setId(1L);
        model.setName("Basic");

        Field front = new Field();
        front.setName("Front");
        front.setOrd(0);

        Field back = new Field();
        back.setName("Back");
        back.setOrd(1);

        model.setFlds(Arrays.asList(front, back));

        // Setup Note
        // Fields split by 0x1F (Unit Separator)
        note = new Note(1L, "guid", "Question\u001FAnswer", 1L);
    }

    @Test
    public void testSimpleRendering() {
        Template tmpl = new Template();
        tmpl.setQfmt("{{Front}}");
        tmpl.setAfmt("{{FrontSide}}\n<hr id=answer>\n{{Back}}");

        String q = renderer.renderQuestion(note, model, tmpl);
        assertEquals("Question", q);

        String a = renderer.renderAnswer(note, model, tmpl);
        assertEquals("Question\n<hr id=answer>\nAnswer", a);
    }

    @Test
    public void testClozeTag() {
        // "cloze:" prefix
        Template tmpl = new Template();
        tmpl.setQfmt("{{cloze:Front}}");

        // My implementation currently returns the raw value for cloze
        String q = renderer.renderQuestion(note, model, tmpl);
        assertEquals("Question", q);
    }

    @Test
    public void testTypeTag() {
        // "type:" prefix
        Template tmpl = new Template();
        tmpl.setQfmt("{{type:Front}}");

        String q = renderer.renderQuestion(note, model, tmpl);
        assertTrue(q.contains("<input type='text'"));
    }

    @Test
    public void testConditionalBlockRendersWhenFieldPresent() {
        Template tmpl = new Template();
        tmpl.setQfmt("{{#Front}}Has content: {{Front}}{{/Front}}");

        String q = renderer.renderQuestion(note, model, tmpl);
        assertEquals("Has content: Question", q);
    }

    @Test
    public void testConditionalBlockHiddenWhenFieldEmpty() {
        // Create note with empty "Front" field
        Note emptyNote = new Note(2L, "guid2", "\u001FAnswer", 1L);

        Template tmpl = new Template();
        tmpl.setQfmt("{{#Front}}Has content{{/Front}}Empty");

        String q = renderer.renderQuestion(emptyNote, model, tmpl);
        assertEquals("Empty", q);
    }

    @Test
    public void testNegativeConditionalBlock() {
        // Create note with empty "Front" field
        Note emptyNote = new Note(3L, "guid3", "\u001FAnswer", 1L);

        Template tmpl = new Template();
        tmpl.setQfmt("{{^Front}}No front content{{/Front}}");

        String q = renderer.renderQuestion(emptyNote, model, tmpl);
        assertEquals("No front content", q);
    }

    @Test
    public void testRenderedCardObject() {
        Template tmpl = new Template();
        tmpl.setQfmt("{{Front}}");
        tmpl.setAfmt("{{FrontSide}}<hr>{{Back}}");
        model.setCss(".card { color: red; }");

        RenderedCard card = renderer.renderCard(note, model, tmpl);

        assertEquals("Question", card.getFields().get("Front"));
        assertEquals("Answer", card.getFields().get("Back"));
        assertEquals("Question", card.getFront());
        assertEquals("Question<hr>Answer", card.getBack());
        assertEquals(".card { color: red; }", card.getCss());
    }

    @Test
    public void testUnknownFieldReturnsEmpty() {
        Template tmpl = new Template();
        tmpl.setQfmt("{{Unknown}}");
        String q = renderer.renderQuestion(note, model, tmpl);
        // New behavior: unknown fields return empty string
        assertEquals("", q);
    }
}
