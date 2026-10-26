package me.albert.timeditem.harness;

import me.albert.timeditem.TimeLore;
import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.api.TimedItemAPI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Guard rails of the public API: blacklisted materials and non-positive
 * durations must be rejected, untimed stacks must read as untimed.
 * (The lane config blacklists BEDROCK on purpose.)
 */
final class ApiFacadeScenario implements Scenario {

    @Override
    public String name() {
        return "api_facade";
    }

    @Override
    public String skipReason(TimedItemPlugin plugin) {
        return null;
    }

    @Override
    public void start(Context ctx) {
        ItemStack bedrock = new ItemStack(Material.BEDROCK);
        ctx.check(TimeLore.isBlacklisted(bedrock, ctx.plugin().pluginConfig()),
                "BEDROCK blacklisted (lane config)");

        try {
            TimedItemAPI.setExpiryFromNow(bedrock, 60L);
            ctx.check(false, "setExpiry on blacklisted material to throw");
        } catch (IllegalArgumentException expected) {
            // correct behaviour
        }
        try {
            TimedItemAPI.setExpiryFromNow(new ItemStack(Material.DIAMOND), 0L);
            ctx.check(false, "setExpiry with zero seconds to throw");
        } catch (IllegalArgumentException expected) {
            // correct behaviour
        }

        ItemStack plain = new ItemStack(Material.STONE);
        ctx.check(!TimedItemAPI.isTimed(plain), "plain stack untimed");
        ctx.check(TimedItemAPI.getType(plain) == null, "untimed type is null");
        ctx.check(TimedItemAPI.getRemainingSeconds(plain) == null, "untimed remaining is null");
        ctx.check(!TimedItemAPI.pause(plain), "pause() rejects untimed stack");
        ctx.finish("guard rails: blacklist + non-positive seconds rejected, untimed reads null");
    }
}
