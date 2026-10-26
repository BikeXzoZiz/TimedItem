package me.albert.timeditem.storage;

import java.util.ArrayList;
import java.util.List;

/**
 * Line format of audit.log, free of Bukkit types so it can be unit
 * tested without a server:
 *
 *   timeMillis|actorUuid|actorName|action|target|itemUid|itemId|details
 *
 * Empty fields mean "none" (console actor, no target, ground item...).
 * Pipe characters inside free-text fields are escaped as \p and
 * backslashes as \\ so one line is always one record.
 */
public final class AuditFormat {

    private static final String ESCAPED_PIPE = "\\p";
    private static final String ESCAPED_BACKSLASH = "\\\\";

    private AuditFormat() {
    }

    public static String serialize(List<AuditRecord> records) {
        StringBuilder out = new StringBuilder();
        for (AuditRecord record : records) {
            out.append(serialize(record)).append('\n');
        }
        return out.toString();
    }

    public static String serialize(AuditRecord record) {
        return record.getTimeMillis()
                + "|" + escape(record.getActorUuid())
                + "|" + escape(record.getActorName())
                + "|" + escape(record.getAction())
                + "|" + escape(record.getTarget())
                + "|" + escape(record.getItemUid())
                + "|" + escape(record.getItemId())
                + "|" + escape(record.getDetails());
    }

    /** Parses whole lines, skipping blanks and malformed lines. */
    public static List<AuditRecord> parse(List<String> lines) {
        List<AuditRecord> out = new ArrayList<AuditRecord>();
        for (String line : lines) {
            AuditRecord record = parse(line);
            if (record != null) {
                out.add(record);
            }
        }
        return out;
    }

    public static AuditRecord parse(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }
        String[] parts = split(line);
        if (parts.length != 8) {
            return null;
        }
        long time;
        try {
            time = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
        return new AuditRecord(time, unescape(parts[1]), unescape(parts[2]), unescape(parts[3]),
                unescape(parts[4]), unescape(parts[5]), unescape(parts[6]), unescape(parts[7]));
    }

    private static String escape(String field) {
        if (field == null) {
            return "";
        }
        return field.replace("\\", ESCAPED_BACKSLASH).replace("|", ESCAPED_PIPE);
    }

    private static String unescape(String field) {
        if (field == null || field.isEmpty()) {
            return null;
        }
        // single pass: \p -> | and \\ -> \
        StringBuilder out = new StringBuilder(field.length());
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (c == '\\' && i + 1 < field.length()) {
                char next = field.charAt(i + 1);
                if (next == 'p') {
                    out.append('|');
                    i++;
                    continue;
                }
                if (next == '\\') {
                    out.append('\\');
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    /** Splits on unescaped pipes only (escape() guarantees the rest is pipe-free). */
    private static String[] split(String line) {
        List<String> parts = new ArrayList<String>(8);
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) {
                current.append(c).append(line.charAt(i + 1));
                i++;
                continue;
            }
            if (c == '|') {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }
}
