package me.albert.timeditem;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Posts expiry notifications to a Discord webhook. Needs no external
 * library: a plain HttpURLConnection POSTs the JSON payload from one
 * background daemon thread (fire and forget). Placeholders are filled
 * synchronously on the expiry thread; every network failure is silent
 * except a rate-limited warning so a broken URL cannot spam the console.
 */
public final class DiscordWebhook {

    private static final long WARN_INTERVAL_MILLIS = 60000L;

    private final TimedItemPlugin plugin;
    private final ExecutorService poster = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "TimedItem-Webhook");
        thread.setDaemon(true);
        return thread;
    });
    private volatile long lastWarnMillis;

    public DiscordWebhook(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Sends the expiry notification, replacing {player} (- for ground
     * expiry), {item} and {world} in the configured message template.
     */
    public void notifyExpiry(ItemStack item, Player player, World world, PluginConfig config) {
        String url = config.getDiscordWebhookUrl();
        if (!config.isDiscordWebhookEnabled() || url == null || url.isEmpty()) {
            return;
        }
        String message = config.getDiscordWebhookMessage()
                .replace("{player}", player == null ? "-" : player.getName())
                .replace("{item}", item.getType().name())
                .replace("{world}", world == null ? "-" : world.getName());
        post(url, config.getDiscordWebhookUsername(), message);
    }

    private void post(final String url, final String username, final String message) {
        final String payload = buildPayload(username, message);
        poster.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                byte[] body = payload.getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(body.length);
                OutputStream out = connection.getOutputStream();
                out.write(body);
                out.flush();
                out.close();
                int status = connection.getResponseCode();
                connection.disconnect();
                if (status < 200 || status >= 300) {
                    warnOnce("Discord webhook responded " + status);
                }
            } catch (Exception e) {
                warnOnce("Discord webhook failed: " + e.getMessage());
            }
        });
    }

    /** Stops the background sender after flushing pending posts. Call on disable. */
    public void close() {
        poster.shutdown();
    }

    /** JSON body of the webhook request (pure - unit tested). */
    static String buildPayload(String username, String content) {
        StringBuilder out = new StringBuilder("{\"content\":");
        appendJsonString(out, content);
        if (username != null && !username.isEmpty()) {
            out.append(",\"username\":");
            appendJsonString(out, username);
        }
        return out.append('}').toString();
    }

    private static void appendJsonString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }

    private void warnOnce(String text) {
        long now = System.currentTimeMillis();
        if (now - lastWarnMillis >= WARN_INTERVAL_MILLIS) {
            lastWarnMillis = now;
            plugin.getLogger().warning(text);
        }
    }
}
