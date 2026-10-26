package me.albert.timeditem.harness;

import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.api.TimedItemAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;

/**
 * The distribution auto-timers against the fake Shopkeepers / CrazyCrates
 * stubs: the stubs fire their foreign trade/prize events with whatever
 * stack the harness hands them (Bridge.nextItem) and store the post-event
 * stack (Bridge.lastResult). Asserts that fresh items get stamped with
 * the configured auto_timer, already-timed items are left alone and
 * blacklisted materials are untouched.
 */
final class AutoTimerScenario implements Scenario {

    private Class<?> shopkeepersBridge;
    private Class<?> cratesBridge;

    @Override
    public String name() {
        return "auto_timer";
    }

    @Override
    public String skipReason(TimedItemPlugin plugin) {
        Plugin shopkeepers = Bukkit.getPluginManager().getPlugin("Shopkeepers");
        Plugin crates = Bukkit.getPluginManager().getPlugin("CrazyCrates");
        if (shopkeepers == null || crates == null) {
            return "needs the FakeShopkeepers + FakeCrazyCrates stub plugins";
        }
        if (plugin.pluginConfig().getShopkeepersAutoTimer().trim().isEmpty()
                || plugin.pluginConfig().getCrazyCratesAutoTimer().trim().isEmpty()) {
            return "lane config has no Hooks.shopkeepers/crazycrates.auto_timer";
        }
        return null;
    }

    @Override
    public int timeoutSeconds() {
        return 15;
    }

    @Override
    public void start(final Context ctx) {
        try {
            shopkeepersBridge = bridgeOf("Shopkeepers", "fake.shopkeepers.Bridge");
            cratesBridge = bridgeOf("CrazyCrates", "fake.crazycrates.Bridge");
        } catch (Throwable t) {
            ctx.finish(false, "stub bridge classes not found: " + t);
            return;
        }

        // 1) fresh stack -> stamped with Hooks.shopkeepers.auto_timer
        // (dispatches must run on the global region - a region thread
        // refuses Bukkit.dispatchCommand on Folia)
        ctx.later(2L, new Runnable() {
            @Override
            public void run() {
                handTo(shopkeepersBridge, new ItemStack(Material.DIAMOND), "fresh");
                dispatch(ctx, "fakesh fire");
                // 2) already timed -> untouched
                ItemStack timed = new ItemStack(Material.EMERALD);
                TimedItemAPI.setExpiryFromNow(timed, 3600L);
                handTo(shopkeepersBridge, timed, "timed");
                dispatch(ctx, "fakesh fire");
                // 3) blacklisted material -> untouched
                handTo(shopkeepersBridge, new ItemStack(Material.BEDROCK), "blacklist");
                dispatch(ctx, "fakesh fire");
                // 4) crate prize -> stamped with Hooks.crazycrates.auto_timer
                handTo(cratesBridge, new ItemStack(Material.GOLD_INGOT), "prize");
                dispatch(ctx, "fakecc fire");

                ctx.later(30L, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // shopkeepers fires 3 trades, crazycrates 1 - results land in fire order
                            ItemStack fresh = resultAt(shopkeepersBridge, 0);
                            ItemStack timedBack = resultAt(shopkeepersBridge, 1);
                            ItemStack blacklisted = resultAt(shopkeepersBridge, 2);
                            ItemStack prize = resultAt(cratesBridge, 0);

                            Long freshRemaining = remaining(fresh);
                            ctx.check(freshRemaining != null && freshRemaining >= 25L && freshRemaining <= 30L,
                                    "fresh shop item stamped ~30s, got " + freshRemaining);
                            Long timedRemaining = remaining(timedBack);
                            ctx.check(timedRemaining != null && timedRemaining >= 3590L,
                                    "already-timed item not re-stamped (~3600s left), got " + timedRemaining);
                            ctx.check(blacklisted == null || !TimedItemAPI.isTimed(blacklisted),
                                    "blacklisted BEDROCK untouched by auto-timer");
                            Long prizeRemaining = remaining(prize);
                            ctx.check(prizeRemaining != null && prizeRemaining >= 40L && prizeRemaining <= 45L,
                                    "crate prize stamped ~45s, got " + prizeRemaining);
                            ctx.finish("auto-timer stamped fresh shop item + crate prize, "
                                    + "timed and blacklisted stacks untouched");
                        } catch (Throwable t) {
                            ctx.finish(false, "reading bridge results failed: " + t);
                        }
                    }
                });
            }
        });
    }

    private static Class<?> bridgeOf(String pluginName, String className) throws Exception {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            throw new IllegalStateException(pluginName + " not loaded");
        }
        return plugin.getClass().getClassLoader().loadClass(className);
    }

    private static void handTo(Class<?> bridge, ItemStack stack, String label) {
        try {
            Field next = bridge.getField("nextItem");
            next.set(null, stack);
        } catch (Throwable t) {
            throw new IllegalStateException("cannot hand " + label + " to bridge", t);
        }
    }

    @SuppressWarnings("unchecked")
    private static ItemStack resultAt(Class<?> bridge, int index) throws Exception {
        Field results = bridge.getField("results");
        java.util.List<ItemStack> list = (java.util.List<ItemStack>) results.get(null);
        return index < list.size() ? list.get(index) : null;
    }

    private static Long remaining(ItemStack stack) {
        return stack == null ? null : TimedItemAPI.getRemainingSeconds(stack);
    }

    private static void dispatch(Context ctx, String command) {
        if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
            ctx.note("command not registered: " + command);
        }
    }
}
