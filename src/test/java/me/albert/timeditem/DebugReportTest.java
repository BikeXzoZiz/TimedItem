package me.albert.timeditem;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure render tests for the debug report: section structure, log
 * inclusion and the privacy rule that no webhook URL can leak.
 */
class DebugReportTest {

    private DebugReport.Snapshot sample() {
        DebugReport.Snapshot s = new DebugReport.Snapshot();
        s.pluginVersion = "2.4.0";
        s.serverVersion = "git-Paper-499 (MC: 1.20.4)";
        s.apiVersion = "1.20.4-R0.1-SNAPSHOT";
        s.folia = false;
        s.javaVersion = "17.0.9";
        s.pdcAvailable = true;
        s.itemFormat = "nbt";
        s.placeholderApi = true;
        s.onlinePlayers = 3;
        s.worldCount = 2;
        s.language = "en";
        s.debugMode = true;
        s.expireCheckEnabled = true;
        s.expireCheckSeconds = 10;
        s.scanWorldItems = false;
        s.ignoreCreative = true;
        s.warningEnabled = true;
        s.actionbarEnabled = true;
        s.metricsEnabled = true;
        s.updateCheckEnabled = true;
        s.recoveryEnabled = true;
        s.recoveryMaxItems = 500;
        s.recoveryKeepDays = 7;
        s.webhookEnabled = true;
        s.blacklistSize = 2;
        s.recoveryEntries = 12;
        s.recoveryFileExists = true;
        s.recoveryFileBytes = 4567;
        s.recoveryFilePath = "plugins" + System.getProperty("file.separator") + "TimedItem"
                + System.getProperty("file.separator") + "recovery.log";
        s.generatedAt = "2026-10-26 18:04:59";
        return s;
    }

    @Test
    void renderContainsAllSections() {
        String report = DebugReport.render(sample(),
                Collections.singletonList("2026-10-26 18:00:00 [INFO] TimedItem enabled"));
        assertTrue(report.contains("TimedItem debug report"));
        assertTrue(report.contains("[Environment]"));
        assertTrue(report.contains("[Settings]"));
        assertTrue(report.contains("[Recovery]"));
        assertTrue(report.contains("[Recent log"));
        assertTrue(report.contains("[End of report]"));
        assertTrue(report.contains("plugin_version: 2.4.0"));
        assertTrue(report.contains("server: git-Paper-499 (MC: 1.20.4)"));
        assertTrue(report.contains("folia: false"));
        assertTrue(report.contains("item_storage: PDC (recovery format tag: nbt)"));
        assertTrue(report.contains("recovery.max_items: 500"));
        assertTrue(report.contains("blacklist.materials.size: 2"));
    }

    @Test
    void renderNeverContainsTheWebhookUrl() {
        String report = DebugReport.render(sample(), Collections.<String>emptyList());
        // the snapshot has no URL field at all - the render can only state
        // whether the webhook is enabled
        assertTrue(report.contains("discord_webhook.enabled: true"));
        assertFalse(report.contains("discord.com"));
        assertFalse(report.contains("webhooks/"));
        assertFalse(report.contains("http"));
    }

    @Test
    void renderIncludesEveryRecentLogLine() {
        String report = DebugReport.render(sample(), Arrays.asList(
                "2026-10-26 18:00:00 [INFO] line one",
                "2026-10-26 18:00:01 [WARNING] line two"));
        assertTrue(report.contains("line one"));
        assertTrue(report.contains("line two"));
    }

    @Test
    void recoveryFileSummaryShowsState() {
        String withFile = DebugReport.render(sample(), Collections.<String>emptyList());
        assertTrue(withFile.contains("entries: 12"));
        assertTrue(withFile.contains("(exists, 4567 bytes)"));

        DebugReport.Snapshot missing = sample();
        missing.recoveryFileExists = false;
        String withoutFile = DebugReport.render(missing, Collections.<String>emptyList());
        assertTrue(withoutFile.contains("(missing)"));
    }

    @Test
    void fileNameMatchesTimestampPattern() {
        String name = DebugReport.fileName();
        assertTrue(name.matches("report-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.txt"),
                "unexpected file name: " + name);
    }

    @Test
    void loreStorageIsReportedWhenPdcIsUnavailable() {
        DebugReport.Snapshot s = sample();
        s.pdcAvailable = false;
        String report = DebugReport.render(s, Collections.<String>emptyList());
        assertTrue(report.contains("item_storage: lore"));
    }
}
