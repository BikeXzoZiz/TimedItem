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

    @Test
    void buildsEmbedWithAllFields() {
        String payload = DiscordWebhook.buildEmbedPayload("TimedItem", "TimedItem",
                "Steve's item expired", 0xff5555, "2026-10-26T12:00:00Z");
        assertEquals("{\"embeds\":[{\"title\":\"TimedItem\",\"description\":\"Steve's item expired\","
                        + "\"color\":16733525,\"timestamp\":\"2026-10-26T12:00:00Z\"}],"
                        + "\"username\":\"TimedItem\"}",
                payload);
    }

    @Test
    void embedWithoutTitleOmitsTitleKey() {
        String payload = DiscordWebhook.buildEmbedPayload("", null, "desc", 0xff5555, null);
        assertEquals("{\"embeds\":[{\"description\":\"desc\",\"color\":16733525}]}", payload);
    }

    @Test
    void embedDescriptionEscapesJson() {
        String payload = DiscordWebhook.buildEmbedPayload(null, "", "a\"b\n", 1, "");
        assertTrue(payload.contains("\"description\":\"a\\\"b\\n\""));
        assertFalse(payload.contains("\"username\""));
    }
}
