package me.albert.timeditem.harness;

import me.albert.timeditem.PdcCompat;
import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.api.TimedItemAPI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * The full timer lifecycle on a plain stack: set expiry, analyze, uid,
 * pause/resume, lifetime, clear.
 */
final class LoreRoundtripScenario implements Scenario {

    @Override
    public String name() {
        return "lore_roundtrip";
    }

    @Override
    public String skipReason(TimedItemPlugin plugin) {
        return null;
    }

    @Override
    public void start(Context ctx) {
        ItemStack stack = new ItemStack(Material.DIAMOND);
        TimedItemAPI.setExpiryFromNow(stack, 3600L);
        ctx.check(TimedItemAPI.isTimed(stack), "isTimed after setExpiryFromNow");
        ctx.check("expire".equals(TimedItemAPI.getType(stack)), "type is expire");
        Long remaining = TimedItemAPI.getRemainingSeconds(stack);
        ctx.check(remaining != null && remaining >= 3590L && remaining <= 3600L,
                "remaining ~3600s, got " + remaining);
        if (PdcCompat.isAvailable()) {
            ctx.check(PdcCompat.readUid(stack) != null, "PDC uid written");
        }
        ctx.check(TimedItemAPI.pause(stack), "pause() accepts a counting item");
        ctx.check("paused".equals(TimedItemAPI.getType(stack)), "type is paused after pause()");
        ctx.check(TimedItemAPI.resume(stack), "resume() accepts a paused item");
        ctx.check("expire".equals(TimedItemAPI.getType(stack)), "type is expire after resume()");

        ItemStack lifetime = new ItemStack(Material.GOLD_INGOT);
        TimedItemAPI.setLifetime(lifetime, 600L);
        ctx.check("lifetime".equals(TimedItemAPI.getType(lifetime)), "setLifetime type");

        TimedItemAPI.clear(stack);
        ctx.check(!TimedItemAPI.isTimed(stack), "untimed after clear()");
        ctx.finish("expiry/pause/resume/lifetime/clear + PDC uid all round-tripped");
    }
}
