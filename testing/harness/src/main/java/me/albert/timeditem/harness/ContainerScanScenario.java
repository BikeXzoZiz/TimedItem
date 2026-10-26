package me.albert.timeditem.harness;

import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.api.TimedItemAPI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

/**
 * Periodic container scanning (3.0+, classic only): a chest holding an
 * expired timed item is emptied by the chunk walk within the configured
 * interval. On Folia this scenario is skipped - the design deliberately
 * replaces the walk with per-entity tracking.
 */
final class ContainerScanScenario implements Scenario {

    private volatile Block chest;
    private boolean placed;
    private boolean contentsDropped;

    @Override
    public String name() {
        return "container_scan";
    }

    @Override
    public String skipReason(TimedItemPlugin plugin) {
        if (F.isFolia()) {
            return "periodic container scan is classic-only by design (Folia uses per-entity tracking)";
        }
        return null;
    }

    @Override
    public int timeoutSeconds() {
        return 40;
    }

    @Override
    public void start(final Context ctx) {
        final ItemStack expired = new ItemStack(Material.GOLD_NUGGET);
        TimedItemAPI.setExpiryFromNow(expired, 1L);
        ctx.laterAt(ctx.base(), 40L, new Runnable() {
            @Override
            public void run() {
                ctx.at(ctx.base(), new Runnable() {
                    @Override
                    public void run() {
                        Block block = ctx.base().getBlock();
                        block.setType(Material.CHEST);
                        chest = block;
                        placed = true;
                        if (block.getState() instanceof Chest) {
                            ((Chest) block.getState()).getInventory().setItem(4, expired);
                            contentsDropped = true;
                        }
                    }
                });
            }
        });
        ctx.cleanup(new Runnable() {
            @Override
            public void run() {
                if (chest != null && chest.getType() == Material.CHEST) {
                    chest.setType(Material.AIR);
                }
            }
        });
    }

    @Override
    public void poll(final Context ctx) {
        if (!placed) {
            return;
        }
        ctx.check(contentsDropped, "chest placed and expired nugget stored");
        if (!contentsDropped) {
            ctx.finish(false, "chest block state was not a chest");
            return;
        }
        ctx.at(ctx.base(), new Runnable() {
            @Override
            public void run() {
                if (chest == null || chest.getType() != Material.CHEST) {
                    ctx.finish(false, "chest block disappeared before the scan ran");
                    return;
                }
                if (!(chest.getState() instanceof Chest)) {
                    ctx.finish(false, "block state no longer a chest");
                    return;
                }
                ItemStack inside = ((Chest) chest.getState()).getInventory().getItem(4);
                if (inside == null || inside.getType() == Material.AIR) {
                    ctx.finish("periodic scan removed the expired nugget from the chest");
                }
            }
        });
    }
}
