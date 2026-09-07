package com.monkey.ktplus.storage.migration.transfer;

import java.util.ArrayList;
import java.util.List;

final class TsvFormat {
    private TsvFormat() {}

    static String escape(Object value) {
        if (value == null) {
            return "\\N";
        }
        String text = String.valueOf(value);
        StringBuilder builder = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                default:
                    builder.append(ch);
                    break;
            }
        }
        return builder.toString();
    }

    static String unescape(String raw) {
        if (raw == null || "\\N".equals(raw)) {
            return null;
        }
        StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\\' && i + 1 < raw.length()) {
                char next = raw.charAt(++i);
                switch (next) {
                    case '\\':
                        builder.append('\\');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 'N':
                        builder.append('\\').append('N');
                        break;
                    default:
                        builder.append('\\').append(next);
                        break;
                }
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    static List<String> splitLine(String line) {
        List<String> parts = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\\' && i + 1 < line.length()) {
                current.append(ch);
                current.append(line.charAt(++i));
                continue;
            }
            if (ch == '\t') {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        parts.add(current.toString());
        return parts;
    }
}
