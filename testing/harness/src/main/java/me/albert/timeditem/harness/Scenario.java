package me.albert.timeditem.harness;

import me.albert.timeditem.TimedItemPlugin;

/**
 * One console-driven test scenario. {@link #start(Context)} sets things
 * up and may finish immediately; the suite polls {@link #poll(Context)}
 * roughly once a second until the context is finished or the scenario
 * times out.
 */
interface Scenario {

    String name();

    /** Null when the scenario should run; otherwise the reason it is skipped. */
    String skipReason(TimedItemPlugin plugin);

    default int timeoutSeconds() {
        return 30;
    }

    void start(Context ctx);

    default void poll(Context ctx) {
    }
}
