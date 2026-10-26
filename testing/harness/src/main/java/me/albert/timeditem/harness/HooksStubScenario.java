package me.albert.timeditem.harness;

import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.inventory.ItemStack;

/**
 * The hook layer end to end against the FakeHooks stub plugins: every
 * adapter must build its item, re-identify it with the same token and
 * describe it. CrazyCrates is build-only by design (prize stacks carry
 * no tag), so only the build is asserted there.
 */
final class HooksStubScenario implements Scenario {

    /** token -> expected itemId() roundtrip (null = build-only adapter). */
    private static final String[][] EXPECTATIONS = {
            {"mmoitems:SWORD:CUTLASS", "mmoitems:SWORD:CUTLASS"},
            {"itemsadder:iasurvival:ruby_sword", "itemsadder:iasurvival:ruby_sword"},
            {"oraxen:ruby_sword", "oraxen:ruby_sword"},
            {"nexo:ruby_sword", "nexo:ruby_sword"},
            {"executableitems:gkit", "executableitems:gkit"},
            {"slimefun:test_item", "slimefun:test_item"},
            {"ecoitems:test_sword", "ecoitems:test_sword"},
            {"crazycrates:basic:prize", null}
    };

    @Override
    public String name() {
        return "hooks_stub";
    }

    @Override
    public String skipReason(TimedItemPlugin plugin) {
        return null;
    }

    @Override
    public int timeoutSeconds() {
        return 10;
    }

    @Override
    public void start(Context ctx) {
        // the plugin's own hook summary decides what CAN be tested here:
        // an adapter the plugin could not activate (e.g. the fake env
        // cannot emulate the MythicLib/io.lumine class timing on Paper)
        // is reported, not failed - real-hooks lanes activate it normally
        String summary = ctx.plugin().hooks().summary();
        ctx.note("hooks summary: " + summary);
        int exercised = 0;
        int skipped = 0;
        int roundtrips = 0;
        for (String[] expectation : EXPECTATIONS) {
            String token = expectation[0];
            String expectedId = expectation[1];
            String prefix = token.substring(0, token.indexOf(':'));
            if (summary.contains(prefix + " (off)")) {
                skipped++;
                ctx.note(prefix + ": adapter inactive in this environment - skipped");
                continue;
            }
            exercised++;
            ItemStack built = ctx.plugin().hooks().build(token);
            ctx.check(built != null, token + " build() non-null (adapter enabled but build failed)");
            if (built == null) {
                continue;
            }
            if (expectedId == null) {
                // CrazyCrates: build-only, prize stacks stay plain vanilla
                ctx.check(ctx.plugin().hooks().itemId(built) == null,
                        token + " itemId() stays null (build-only adapter)");
            } else {
                String id = ctx.plugin().hooks().itemId(built);
                ctx.check(expectedId.equals(id),
                        token + " identify roundtrip, got: " + id);
                if (expectedId.equals(id)) {
                    roundtrips++;
                }
            }
            String describe = ctx.plugin().hooks().describe(built);
            ctx.check(describe != null && !describe.trim().isEmpty(),
                    token + " describe() non-empty");
        }
        // alias prefixes reach the same adapters
        if (!summary.contains("itemsadder (off)")) {
            ctx.check(ctx.plugin().hooks().build("ia:iasurvival:ruby_sword") != null,
                    "alias 'ia:' builds through ItemsAdderAdapter");
        }
        if (!summary.contains("crazycrates (off)")) {
            ctx.check(ctx.plugin().hooks().build("cc:basic:prize") != null,
                    "alias 'cc:' builds through CrazyCratesAdapter");
        }
        ctx.finish(roundtrips + "/" + (exercised - 1 <= 0 ? 0 : exercised - 1)
                + " active adapters identified their items"
                + (skipped > 0 ? " (" + skipped + " inactive in this env)" : ""));
    }
}
