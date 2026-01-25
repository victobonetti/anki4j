package com.anki4j.renderer;

import java.util.Map;

/**
 * Represents a fully rendered Anki card.
 * Contains the raw field values, rendered HTML for front/back, and CSS.
 */
public class RenderedCard {
    private final Map<String, String> fields;
    private final String front;
    private final String back;
    private final String css;

    public RenderedCard(Map<String, String> fields, String front, String back, String css) {
        this.fields = fields;
        this.front = front;
        this.back = back;
        this.css = css;
    }

    /**
     * @return Key-value map of field names to their raw content.
     */
    public Map<String, String> getFields() {
        return fields;
    }

    /**
     * @return Fully rendered front (question) HTML.
     */
    public String getFront() {
        return front;
    }

    /**
     * @return Fully rendered back (answer) HTML.
     */
    public String getBack() {
        return back;
    }

    /**
     * @return Model CSS for standalone rendering in WebViews.
     */
    public String getCss() {
        return css;
    }
}
