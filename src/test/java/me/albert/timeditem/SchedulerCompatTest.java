package me.albert.timeditem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * On a plain JVM the Folia class probe must fail and the classic Bukkit
 * path must be selected; resolution is cached so repeat calls stay
 * consistent.
 */
class SchedulerCompatTest {

    @Test
    void plainJvmIsNotFolia() {
        assertFalse(SchedulerCompat.isFolia());
    }

    @Test
    void detectionIsResolvedOnceAndStable() {
        boolean first = SchedulerCompat.isFolia();
        assertFalse(SchedulerCompat.isFolia() != first);
    }
}
