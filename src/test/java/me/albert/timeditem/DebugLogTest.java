package me.albert.timeditem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure java.util.logging tests for the debug log capture: the ring
 * buffer, the verbose channel and throwable handling - no Bukkit needed.
 */
class DebugLogTest {

    private final Logger logger = Logger.getLogger("DebugLogTest");

    @BeforeEach
    void setUp() {
        // keep the test output clean; capture goes through our handler only
        logger.setUseParentHandlers(false);
        DebugLog.clear();
        DebugLog.setEnabled(false);
        DebugLog.install(logger);
    }

    @AfterEach
    void tearDown() {
        DebugLog.uninstall();
        DebugLog.clear();
    }

    @Test
    void capturesLogLinesInOrderWithLevel() {
        logger.info("first message");
        logger.warning("second message");
        List<String> lines = DebugLog.snapshot();
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("[INFO] first message"), lines.get(0));
        assertTrue(lines.get(1).contains("[WARNING] second message"), lines.get(1));
    }

    @Test
    void bufferKeepsOnlyTheLastEntries() {
        for (int i = 0; i < DebugLog.CAPACITY + 50; i++) {
            logger.info("line-" + i);
        }
        List<String> lines = DebugLog.snapshot();
        assertEquals(DebugLog.CAPACITY, lines.size());
        assertTrue(lines.get(0).contains("line-50"), lines.get(0));
        assertTrue(lines.get(lines.size() - 1).contains("line-" + (DebugLog.CAPACITY + 49)));
        assertFalse(lines.get(0).contains("line-0"));
    }

    @Test
    void verboseOnlyLogsWhenEnabled() {
        DebugLog.verbose("hidden");
        assertEquals(0, DebugLog.size());
        DebugLog.setEnabled(true);
        DebugLog.verbose("shown");
        assertEquals(1, DebugLog.size());
        assertTrue(DebugLog.snapshot().get(0).contains("[Debug] shown"));
    }

    @Test
    void verboseDeduplicatesRepeatsWithinTheWindow() {
        DebugLog.setEnabled(true);
        DebugLog.verbose("repeat");
        DebugLog.verbose("repeat");
        assertEquals(1, DebugLog.size());
        DebugLog.verbose("repeat with a change");
        assertEquals(2, DebugLog.size());
    }

    @Test
    void thrownExceptionsAreCapturedAsSummary() {
        logger.log(Level.WARNING, "it broke", new RuntimeException("boom"));
        List<String> lines = DebugLog.snapshot();
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("it broke"), lines.get(0));
        assertTrue(lines.get(0).contains("thrown: java.lang.RuntimeException: boom"), lines.get(0));
    }

    @Test
    void fullStackTraceOnlyWhileDebugModeIsOn() {
        logger.log(Level.WARNING, "it broke", new RuntimeException("boom"));
        assertFalse(DebugLog.snapshot().get(0).contains("at java."));
        DebugLog.setEnabled(true);
        logger.log(Level.WARNING, "it broke again", new RuntimeException("boom"));
        assertTrue(DebugLog.snapshot().get(1).contains("at java."));
    }

    @Test
    void uninstallDetachesTheCaptureHandler() {
        DebugLog.uninstall();
        logger.info("not captured");
        assertEquals(0, DebugLog.size());
    }
}
