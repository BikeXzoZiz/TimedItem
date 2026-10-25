package me.albert.timeditem;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Debug mode and in-memory log capture for bug reports.
 *
 * A capture Handler is attached to the plugin logger in onEnable and
 * removed in onDisable, so every line the plugin logs (warnings,
 * migration notices, scheduler failures, ...) lands in a bounded ring
 * buffer that /ti debug export can dump into a report - without touching
 * any of the individual log call sites.
 *
 * The verbose channel adds pipeline detail that otherwise stays silent
 * (serialization fallbacks, expiry events, recovery writes) and only
 * exists while debug mode is on. The flag is a session switch: /ti debug
 * on|off changes it, and it resets to Settings.debug on reload/restart.
 *
 * All state is static because classes without a plugin reference
 * (PdcCompat, ItemSerializer) call verbose from anywhere, on any thread.
 */
public final class DebugLog {

    /** Captured log lines kept for the export report. */
    public static final int CAPACITY = 500;
    /** Identical verbose lines inside this window collapse into one (a hot-path failure must not spam the console). */
    private static final long VERBOSE_DEDUPE_MILLIS = 5000;
    private static final int MAX_STACK_TRACE_CHARS = 4000;

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Deque<String> buffer = new ArrayDeque<String>();
    private static volatile boolean enabled;
    private static volatile Logger pluginLogger;
    private static volatile String lastVerbose;
    private static volatile long lastVerboseMillis;

    private DebugLog() {
    }

    /** Starts capturing the plugin's log output; safe to call again (replaces the old handler). */
    public static void install(Logger logger) {
        uninstall();
        pluginLogger = logger;
        logger.addHandler(new CaptureHandler());
    }

    /** Stops capturing; called on disable so /reload cannot leak the handler into a new plugin instance. */
    public static void uninstall() {
        Logger logger = pluginLogger;
        pluginLogger = null;
        lastVerbose = null;
        if (logger != null) {
            for (Handler handler : logger.getHandlers()) {
                if (handler instanceof CaptureHandler) {
                    logger.removeHandler(handler);
                }
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static synchronized int size() {
        return buffer.size();
    }

    /** Drops everything captured so far (test isolation between cases). */
    static synchronized void clear() {
        buffer.clear();
        lastVerbose = null;
    }

    /** Copy of the captured lines, oldest first. */
    public static synchronized List<String> snapshot() {
        return new ArrayList<String>(buffer);
    }

    /**
     * Pipeline detail that only exists in debug mode. Prints through the
     * plugin logger - which is also what feeds the capture buffer - and
     * is dropped completely while debug mode is off.
     */
    public static void verbose(String message) {
        if (!enabled) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (DebugLog.class) {
            if (message.equals(lastVerbose) && now - lastVerboseMillis < VERBOSE_DEDUPE_MILLIS) {
                return;
            }
            lastVerbose = message;
            lastVerboseMillis = now;
        }
        Logger logger = pluginLogger;
        if (logger != null) {
            logger.info("[Debug] " + message);
        } else {
            store("[INFO] [Debug] " + message);
        }
    }

    private static void store(String line) {
        String entry = "[" + LocalDateTime.now().format(TIMESTAMP) + "] " + line;
        synchronized (buffer) {
            while (buffer.size() >= CAPACITY) {
                buffer.pollFirst();
            }
            buffer.addLast(entry);
        }
    }

    private static String stackTrace(Throwable thrown) {
        StringWriter out = new StringWriter();
        thrown.printStackTrace(new PrintWriter(out));
        String trace = out.toString();
        if (trace.length() > MAX_STACK_TRACE_CHARS) {
            return trace.substring(0, MAX_STACK_TRACE_CHARS) + "\n...(truncated)";
        }
        return trace;
    }

    /** Sits on the plugin logger and copies every record into the buffer. */
    private static final class CaptureHandler extends Handler {

        @Override
        public void publish(LogRecord record) {
            if (record == null) {
                return;
            }
            String message = record.getMessage();
            Throwable thrown = record.getThrown();
            if (thrown != null) {
                message = message + "\n  thrown: " + thrown;
                // full traces only while debug mode is on, so the buffer
                // cannot fill with stack traces from a repeating failure
                if (DebugLog.isEnabled()) {
                    message = message + "\n" + stackTrace(thrown);
                }
            }
            store("[" + record.getLevel().getName() + "] " + message);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
