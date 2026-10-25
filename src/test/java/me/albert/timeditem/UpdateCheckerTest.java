package me.albert.timeditem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {

    @Test
    void newerVersionIsDetected() {
        assertTrue(UpdateChecker.isNewerVersion("2.3.0", "v2.3.1"));
        assertTrue(UpdateChecker.isNewerVersion("2.3.0", "2.10.0"));
        assertTrue(UpdateChecker.isNewerVersion("2.3", "2.3.1"));
    }

    @Test
    void sameVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewerVersion("2.3.0", "v2.3.0"));
        assertFalse(UpdateChecker.isNewerVersion("2.3.0", "2.3"));
    }

    @Test
    void olderVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewerVersion("2.3.0", "v2.2.9"));
    }

    @Test
    void suffixesAreIgnored() {
        assertTrue(UpdateChecker.isNewerVersion("2.3.0", "v2.4.0-beta.1"));
        assertFalse(UpdateChecker.isNewerVersion("2.3.0", "v2.3.0+build.5"));
    }

    @Test
    void garbageNeverCountsAsNewer() {
        assertFalse(UpdateChecker.isNewerVersion("2.3.0", "release-candidate"));
        assertFalse(UpdateChecker.isNewerVersion("", "2.4.0"));
        assertFalse(UpdateChecker.isNewerVersion("2.3.0", null));
        assertFalse(UpdateChecker.isNewerVersion(null, "2.4.0"));
    }
}
