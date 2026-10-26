package me.albert.timeditem;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.util.logging.Level;

/**
 * Periodic container scanning (3.0+, Settings.container_scan mode:
 * "periodic"/"both"): walks every loaded chunk's tile entities and
 * expires due items in chests, barrels, shulkers, furnaces and so on -
 * containers nobody opens are covered too.
 *
 * Classic servers only (a global task may not read other regions'
 * chunks on Folia). Heavy on purpose: its own interval (default 60s)
 * and off by default. Expiry here has no player - recovery, webhook,
 * commands without {player} and the statistics still apply.
 */
public final class ContainerScanTask implements Runnable {

    private final TimedItemPlugin plugin;

    public ContainerScanTask(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            runScan();
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Container scan failed: " + t, t);
        }
    }

    private void runScan() {
        PluginConfig config = plugin.pluginConfig();
        LocalDateTime now = LocalDateTime.now();
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof org.bukkit.block.Container)) {
                        continue;
                    }
                    Inventory inventory = ((org.bukkit.block.Container) state).getInventory();
                    scan(inventory, config, now, state);
                }
            }
        }
    }

    private void scan(Inventory inventory, PluginConfig config, LocalDateTime now,
                      BlockState state) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            TimeLore.TimerInfo info = TimeLore.analyze(item, config);
            if (info == null || info.getType() != TimeLore.Type.EXPIRE
                    || info.getExpiry() == null || !info.getExpiry().isBefore(now)) {
                continue;
            }
            final int index = slot;
            plugin.expiryService().expire(item, null, state.getLocation(),
                    state.getWorld(), () -> inventory.setItem(index, null), false, config);
        }
    }
}
