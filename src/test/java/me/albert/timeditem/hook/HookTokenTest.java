package me.albert.timeditem.hook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins the grammar of /ti give custom item tokens: the prefix becomes
 * the lowercase plugin id, everything after the first ':' is the
 * adapter's id parts, and tokens without a usable prefix are rejected
 * (they fall back to the legacy give-held-item flow).
 */
class HookTokenTest {

    @Test
    void splitsPluginAndMultiPartId() {
        assertArrayEquals(new String[] {"mmoitems", "SWORD", "CUTLASS"},
                HookManager.parseToken("mmoitems:SWORD:CUTLASS"));
        assertArrayEquals(new String[] {"itemsadder", "iasurvival", "ruby_sword"},
                HookManager.parseToken("itemsadder:iasurvival:ruby_sword"));
    }

    @Test
    void lowercasesThePluginPrefixOnly() {
        assertArrayEquals(new String[] {"ei", "GKIT"},
                HookManager.parseToken("EI:GKIT"));
    }

    @Test
    void rejectsTokensWithoutAUsablePrefix() {
        assertNull(HookManager.parseToken(null));
        assertNull(HookManager.parseToken("plain30d")); // legacy duration flow
        assertNull(HookManager.parseToken(":item"));
        assertNull(HookManager.parseToken("mmoitems:"));
    }
}
