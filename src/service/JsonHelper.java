package service;

import java.util.Map;
import java.util.HashMap;

/**
 * =============================================================================
 * Class: JsonHelper
 * Concept: Zero-dependency JSON parser & utility
 * Converts simple flat JSON payloads into key-value maps and escapes strings.
 * =============================================================================
 */
public class JsonHelper {

    /**
     * Parses simple flat JSON object into Map<String, String>.
     * Example: {"id":"SKU-101","stock":25,"name":"Milk"} -> {"id":"SKU-101","stock":"25","name":"Milk"}
     */
    public static Map<String, String> parseJsonObject(String json) {
        Map<String, String> result = new HashMap<>();
        if (json == null) return result;

        String s = json.trim();
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);
        s = s.trim();

        if (s.isEmpty()) return result;

        boolean inQuotes = false;
        StringBuilder currentToken = new StringBuilder();
        java.util.List<String> pairs = new java.util.ArrayList<>();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
                currentToken.append(c);
            } else if (c == ',' && !inQuotes) {
                pairs.add(currentToken.toString().trim());
                currentToken.setLength(0);
            } else {
                currentToken.append(c);
            }
        }
        if (currentToken.length() > 0) {
            pairs.add(currentToken.toString().trim());
        }

        for (String pair : pairs) {
            int colonIdx = -1;
            boolean inQ = false;
            for (int i = 0; i < pair.length(); i++) {
                char c = pair.charAt(i);
                if (c == '\"' && (i == 0 || pair.charAt(i - 1) != '\\')) {
                    inQ = !inQ;
                } else if (c == ':' && !inQ) {
                    colonIdx = i;
                    break;
                }
            }

            if (colonIdx != -1) {
                String key = stripQuotes(pair.substring(0, colonIdx).trim());
                String val = stripQuotes(pair.substring(colonIdx + 1).trim());
                result.put(key, val);
            }
        }

        return result;
    }

    private static String stripQuotes(String str) {
        if (str == null) return "";
        str = str.trim();
        while (str.startsWith("\"") || str.startsWith("\\\"")) {
            if (str.startsWith("\\\"")) str = str.substring(2);
            else str = str.substring(1);
        }
        while (str.endsWith("\"") || str.endsWith("\\\"")) {
            if (str.endsWith("\\\"")) str = str.substring(0, str.length() - 2);
            else str = str.substring(0, str.length() - 1);
        }
        return str.replace("\\\"", "\"").replace("\\\\", "\\").trim();
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
