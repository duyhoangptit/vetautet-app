package com.vetautet.app.infrastructure.notification;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
* Renders notification template content by replacing {@code {{variable_name}}}
* placeholders with actual values from the provided map.
*
* <p>Replacement rules:
* <ul>
*   <li>All occurrences of {@code {{key}}} are replaced.</li>
*   <li>If a key is present in the map but has a {@code null} value, it is replaced with an empty string.</li>
*   <li>If a key is absent from the map, the placeholder is left as-is.</li>
*   <li>If the template or the variables map is {@code null}, the template is returned unchanged.</li>
* </ul>
*/
@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    public String render(String template, Map<String, String> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (variables.containsKey(key)) {
                String value = variables.get(key);
                matcher.appendReplacement(result, Matcher.quoteReplacement(value != null ? value : ""));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
 