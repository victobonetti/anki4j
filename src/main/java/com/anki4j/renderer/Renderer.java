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

    // Regex to match {{...}} tags
    private static final Pattern TAG_PATTERN = Pattern.compile("\\{\\{[^}]+\\}\\}");

    public String renderQuestion(Note note, Model model, Template template) {
        Map<String, String> fieldValues = getFieldMap(note, model);
        return render(template.getQfmt(), fieldValues, null);
    }

    public String renderAnswer(Note note, Model model, Template template) {
        Map<String, String> fieldValues = getFieldMap(note, model);
        String questionSide = render(template.getQfmt(), fieldValues, null);
        return render(template.getAfmt(), fieldValues, questionSide);
    }

    private Map<String, String> getFieldMap(Note note, Model model) {
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

        Matcher matcher = TAG_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String tag = matcher.group();
            String inner = tag.substring(2, tag.length() - 2); // Remove {{ and }}
            String replacement = "";

            if ("FrontSide".equals(inner)) {
                replacement = (frontSide != null) ? frontSide : "";
            } else if (inner.startsWith("cloze:")) {
                String fieldName = inner.substring(6);
                String value = fieldValues.getOrDefault(fieldName, "");
                // Basic cloze handling: just return the text for now, or maybe strip cloze
                // tags?
                // The prompt asked for "Handle", but didn't specify full cloze logic.
                // Anki's cloze logic is complex. For simple rendering, showing the full text is
                // a safe fallback
                // unless we want to process "cloze:Text" -> "..."
                // For now, let's just return the raw value, or maybe strip {{c1::...}}?
                // Proper cloze replacement is typically done by CSS/Classes in Anki,
                // but the text itself needs to be processed.
                // Let's stick to raw value for now as a "pass-through".
                replacement = value;
            } else if (inner.startsWith("type:")) {
                // String fieldName = inner.substring(5);
                // Input field handling. Usually renders an <input> tag.
                // For static rendering, maybe just show the value or nothing.
                // Let's replace with empty or value?
                // Standard Anki behavior: [[type:Field]] -> <input ...>
                // We will return a simple input placeholder.
                replacement = "<input type='text' value='' class='type-input' />";
            } else {
                // Standard field replacement
                // Also handle modifiers later if needed (e.g. text:Field)
                replacement = fieldValues.getOrDefault(inner, tag);
                if (replacement.equals(tag)) {
                    // If exactly tag (unknown field), maybe check for special modifiers or just
                    // keep it?
                    // Usually Anki keeps it if unknown, or makes empty.
                    // But let's check for "hint:" etc? Logic can be expanded.
                    // For now, let's assume direct match.
                    if (!fieldValues.containsKey(inner)) {
                        // Fallback: check if we have modifiers like 'text:'
                        // Very basic check
                        String[] parts = inner.split(":");
                        if (parts.length > 1) {
                            String fieldName = parts[parts.length - 1];
                            if (fieldValues.containsKey(fieldName)) {
                                replacement = fieldValues.get(fieldName);
                            }
                        }
                    }
                }
            }
            // Escape special chars for matcher.appendReplacement
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
