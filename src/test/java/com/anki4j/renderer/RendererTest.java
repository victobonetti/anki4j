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
    public void testUnknownField() {
        Template tmpl = new Template();
        tmpl.setQfmt("{{Unknown}}");
        String q = renderer.renderQuestion(note, model, tmpl);
        // Should preserve tag if unknown (default Anki behavior usually, or empty)
        // My implementation returns tag if not found
        assertEquals("{{Unknown}}", q);
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
}
