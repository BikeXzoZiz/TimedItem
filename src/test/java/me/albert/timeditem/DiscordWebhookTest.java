package me.albert.timeditem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordWebhookTest {

    @Test
    void buildsContentAndUsername() {
        assertEquals("{\"content\":\"hello\",\"username\":\"TimedItem\"}",
                DiscordWebhook.buildPayload("TimedItem", "hello"));
    }

    @Test
    void emptyUsernameIsOmitted() {
        assertEquals("{\"content\":\"hi\"}", DiscordWebhook.buildPayload("", "hi"));
        assertEquals("{\"content\":\"hi\"}", DiscordWebhook.buildPayload(null, "hi"));
    }

    @Test
    void escapesJsonSpecialCharacters() {
        String payload = DiscordWebhook.buildPayload("Bot", "say \"hi\"\n\\path\t(tab)");
        assertTrue(payload.contains("say \\\"hi\\\"\\n\\\\path\\t(tab)"));
        assertFalse(payload.endsWith("\n"));
    }

    @Test
    void escapesControlCharacters() {
        String payload = DiscordWebhook.buildPayload("Bot", "a\u0001b");
        assertTrue(payload.contains("\\u0001"));
    }
}
