package com.anki4j.renderer;

import com.anki4j.model.Field;
import com.anki4j.model.Model;
import com.anki4j.model.Note;
import com.anki4j.model.Template;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Renderer {

    // Regex for conditional blocks: {{#FieldName}}...{{/FieldName}}
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile(
            "\\{\\{#([^}]+)\\}\\}(.*?)\\{\\{/\\1\\}\\}", Pattern.DOTALL);

    // Regex for negative conditional blocks: {{^FieldName}}...{{/FieldName}}
    private static final Pattern NEGATIVE_CONDITIONAL_PATTERN = Pattern.compile(
            "\\{\\{\\^([^}]+)\\}\\}(.*?)\\{\\{/\\1\\}\\}", Pattern.DOTALL);

    // Regex to match simple {{...}} tags
    private static final Pattern TAG_PATTERN = Pattern.compile("\\{\\{[^}#/^]+\\}\\}");

    /**
     * Renders a full RenderedCard object containing fields, front, back, and CSS.
     */
    public RenderedCard renderCard(Note note, Model model, Template template) {
        Map<String, String> fieldValues = getFieldMap(note, model);
        String front = render(template.getQfmt(), fieldValues, null);
        String back = render(template.getAfmt(), fieldValues, front);
        String css = model.getCss() != null ? model.getCss() : "";
        return new RenderedCard(fieldValues, front, back, css);
    }

    public String renderQuestion(Note note, Model model, Template template) {
        Map<String, String> fieldValues = getFieldMap(note, model);
        return render(template.getQfmt(), fieldValues, null);
    }

    public String renderAnswer(Note note, Model model, Template template) {
        Map<String, String> fieldValues = getFieldMap(note, model);
        String questionSide = render(template.getQfmt(), fieldValues, null);
        return render(template.getAfmt(), fieldValues, questionSide);
    }

    /**
     * Creates a field map from Note content using Model field definitions.
     */
    public Map<String, String> getFieldMap(Note note, Model model) {
        String[] rawValues = note.getFields().split("\u001F"); // Unit Separator 0x1F
        Map<String, String> map = new HashMap<>();

        for (int i = 0; i < model.getFlds().size(); i++) {
            Field field = model.getFlds().get(i);
            String value = (i < rawValues.length) ? rawValues[i] : "";
            map.put(field.getName(), value);
        }
        return map;
    }

    private String render(String template, Map<String, String> fieldValues, String frontSide) {
        if (template == null)
            return "";

        // Pass 1: Process positive conditional blocks {{#Field}}...{{/Field}}
        String result = processConditionals(template, fieldValues, false);

        // Pass 2: Process negative conditional blocks {{^Field}}...{{/Field}}
        result = processConditionals(result, fieldValues, true);

        // Pass 3: Substitute simple tags
        result = substituteSimpleTags(result, fieldValues, frontSide);

        return result;
    }

    private String processConditionals(String template, Map<String, String> fieldValues, boolean negative) {
        Pattern pattern = negative ? NEGATIVE_CONDITIONAL_PATTERN : CONDITIONAL_PATTERN;
        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String fieldName = matcher.group(1).trim();
            String blockContent = matcher.group(2);

            String value = fieldValues.getOrDefault(fieldName, "");
            boolean fieldHasContent = value != null && !value.trim().isEmpty();

            String replacement;
            if (negative) {
                // {{^Field}} - render if field IS empty
                replacement = fieldHasContent ? "" : blockContent;
            } else {
                // {{#Field}} - render if field is NOT empty
                replacement = fieldHasContent ? blockContent : "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String substituteSimpleTags(String template, Map<String, String> fieldValues, String frontSide) {
        Matcher matcher = TAG_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String tag = matcher.group();
            String inner = tag.substring(2, tag.length() - 2).trim(); // Remove {{ and }}
            String replacement = "";

            if ("FrontSide".equals(inner)) {
                replacement = (frontSide != null) ? frontSide : "";
            } else if (inner.startsWith("cloze:")) {
                String fieldName = inner.substring(6);
                replacement = fieldValues.getOrDefault(fieldName, "");
            } else if (inner.startsWith("type:")) {
                replacement = "<input type='text' value='' class='type-input' />";
            } else if (inner.startsWith("hint:")) {
                String fieldName = inner.substring(5);
                String value = fieldValues.getOrDefault(fieldName, "");
                if (!value.isEmpty()) {
                    replacement = "<a class='hint' href='#' onclick='this.style.display=\"none\";document.getElementById(\"hint_"
                            + fieldName + "\").style.display=\"block\";return false;'>Show " + fieldName
                            + "</a><div id='hint_" + fieldName + "' style='display:none'>" + value + "</div>";
                }
            } else {
                // Standard field replacement or modifiers like text:Field
                replacement = fieldValues.getOrDefault(inner, "");
                if (replacement.isEmpty() && inner.contains(":")) {
                    String[] parts = inner.split(":");
                    if (parts.length > 1) {
                        String fieldName = parts[parts.length - 1];
                        replacement = fieldValues.getOrDefault(fieldName, "");
                    }
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
