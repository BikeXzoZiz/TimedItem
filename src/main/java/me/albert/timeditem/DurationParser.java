package me.albert.timeditem;

import java.util.Locale;
import java.util.Map;

/**
 * Parses duration input such as "1d12h30m" (or the legacy two-token
 * "120 s" form, joined by the caller) into a number of seconds using the
 * unit labels configured in DateFormat.s/m/h/d.
 */
public final class DurationParser {

    /** Reject anything beyond 5 years - almost certainly a typo. */
    private static final long MAX_SECONDS = 157680000L;

    private DurationParser() {
    }

    /** @return total seconds, or -1 when the input is not a valid duration */
    public static long parse(String raw, Map<String, Long> unitSeconds) {
        if (raw == null) {
            return -1;
        }
        String input = raw.replace(" ", "").toLowerCase(Locale.ROOT);
        if (input.isEmpty()) {
            return -1;
        }
        long total = 0;
        int index = 0;
        while (index < input.length()) {
            int digitsStart = index;
            while (index < input.length() && isDigit(input.charAt(index))) {
                index++;
            }
            if (index == digitsStart) {
                return -1; // expected a number here
            }
            long amount;
            try {
                amount = Long.parseLong(input.substring(digitsStart, index));
            } catch (NumberFormatException e) {
                return -1;
            }
            int unitStart = index;
            while (index < input.length() && !isDigit(input.charAt(index))) {
                index++;
            }
            Long multiplier = unitSeconds.get(input.substring(unitStart, index));
            if (multiplier == null) {
                return -1; // unknown unit
            }
            total += amount * multiplier;
            if (total > MAX_SECONDS) {
                return -1;
            }
        }
        return total > 0 ? total : -1;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
