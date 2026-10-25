package me.albert.timeditem;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks GitHub Releases for a newer TimedItem version. The HTTP request
 * runs async; joining admins are notified on the main thread when an
 * update was found. Every failure is silent - an offline server or a
 * rate-limited API must never make noise.
 */
public final class UpdateChecker implements Listener {

    private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");

    private final TimedItemPlugin plugin;
    private volatile String latestVersion;

    public UpdateChecker(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    /** Starts the async release check (console notification only). */
    public void check() {
        if (!plugin.pluginConfig().isUpdateCheckEnabled()) {
            return;
        }
        String repository = plugin.pluginConfig().getUpdateCheckRepository();
        SchedulerCompat.runAsync(plugin, () -> {
            String latest = fetchLatestTag(repository);
            if (latest == null) {
                return;
            }
            latestVersion = latest;
            String current = plugin.getDescription().getVersion();
            if (isNewerVersion(current, latest)) {
                plugin.getLogger().info("Update available: TimedItem " + latest
                        + " is out (you are running " + current + ").");
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String latest = latestVersion;
        if (latest == null || !isNewerVersion(plugin.getDescription().getVersion(), latest)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("timeditem.admin")) {
            return;
        }
        SchedulerCompat.runAtPlayerLater(plugin, player, () -> {
            if (player.isOnline()) {
                player.sendMessage(plugin.pluginConfig().getPrefix()
                        + plugin.pluginConfig().getUpdateAvailable()
                        .replace("{version}", latest)
                        .replace("{url}", "https://github.com/"
                                + plugin.pluginConfig().getUpdateCheckRepository() + "/releases"));
            }
        }, 40L);
    }

    /** Fetches the latest release tag from the GitHub API, or null on any failure. */
    private String fetchLatestTag(String repository) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(
                    "https://api.github.com/repos/" + repository + "/releases/latest").openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "TimedItem-UpdateChecker");
            if (connection.getResponseCode() != 200) {
                return null;
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder json = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                json.append(buffer, 0, read);
            }
            reader.close();
            Matcher matcher = TAG_NAME.matcher(json.toString());
            return matcher.find() ? matcher.group(1) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Pure version comparison: true when {@code tag} is a strictly newer
     * release than {@code current}. Segments compare numerically and a
     * leading "v" is ignored (v2.3.1 vs 2.3.0); anything unparseable
     * returns false.
     */
    static boolean isNewerVersion(String current, String tag) {
        int[] currentParts = parse(current);
        int[] tagParts = parse(tag);
        if (currentParts == null || tagParts == null) {
            return false;
        }
        int length = Math.max(currentParts.length, tagParts.length);
        for (int i = 0; i < length; i++) {
            int a = i < currentParts.length ? currentParts[i] : 0;
            int b = i < tagParts.length ? tagParts[i] : 0;
            if (b != a) {
                return b > a;
            }
        }
        return false;
    }

    private static int[] parse(String version) {
        if (version == null) {
            return null;
        }
        String cleaned = version.trim().toLowerCase(Locale.ROOT);
        if (cleaned.startsWith("v")) {
            cleaned = cleaned.substring(1);
        }
        // drop suffixes such as -beta or +build
        int cut = cleaned.indexOf('-');
        if (cut >= 0) {
            cleaned = cleaned.substring(0, cut);
        }
        cut = cleaned.indexOf('+');
        if (cut >= 0) {
            cleaned = cleaned.substring(0, cut);
        }
        String[] segments = cleaned.split("\\.");
        int[] parts = new int[segments.length];
        for (int i = 0; i < segments.length; i++) {
            try {
                parts[i] = Integer.parseInt(segments[i].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return parts;
    }
}
