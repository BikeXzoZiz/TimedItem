package me.albert.timeditem;

import me.albert.timeditem.api.ItemExpireEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The single expiry pipeline shared by every context: player-bound
 * events, the periodic inventory scan and ground/frame/armor-stand
 * expiry. Fires the cancellable {@link ItemExpireEvent}, records the
 * recovery entry, removes the item and runs the configured effects and
 * commands.
 *
 * The player is null for ground expiry: those runs hand the sound and
 * particles to the nearest player instead and skip commands containing
 * {player} (there is no player to substitute).
 */
public final class ExpiryService {

    /** Radius in which a player sees and hears a ground expiry. */
    private static final double GROUND_EFFECT_RADIUS = 20.0;

    private final TimedItemPlugin plugin;

    public ExpiryService(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Expires one item. Fires the cancellable {@link ItemExpireEvent}
     * first; when cancelled nothing happens and false is returned.
     * Records the item in the recovery log before removal.
     *
     * @param player       owning player, or null for ground expiry
     * @param effectCenter where the vanish effects originate (the player's
     *                     or the item's location); null means no effects
     * @param world        world stored in the recovery entry, or null
     * @param removal      removes the item from its slot or entity
     * @param armor        true when the expired item was a worn armor piece
     */
    public boolean expire(ItemStack item, Player player, Location effectCenter, World world,
                          Runnable removal, boolean armor, PluginConfig config) {
        ItemExpireEvent event = new ItemExpireEvent(item, player);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        DebugLog.verbose("Expired " + item.getType()
                + (player != null ? " from " + player.getName() : " (ground/frame)")
                + (world != null ? " in " + world.getName() : ""));
        plugin.recoveryStore().record(item, player, world);
        removal.run();
        Player viewer = player;
        if (viewer == null && effectCenter != null && !SchedulerCompat.isFolia()) {
            // On Folia reading other players' positions cross-region is not
            // safe, so ground expiry there has no nearby-viewer effects.
            viewer = nearestPlayer(effectCenter, GROUND_EFFECT_RADIUS);
        }
        if (viewer != null && config.isSoundEnabled()) {
            viewer.playSound(viewer.getLocation(), config.getSoundName(),
                    config.getSoundVolume(), config.getSoundPitch());
        }
        if (viewer != null && config.isParticleEnabled()) {
            if (player != null) {
                ParticleCompat.spawn(viewer, config.getParticleName(), config.getParticleCount());
            } else {
                ParticleCompat.spawnAt(viewer, effectCenter, config.getParticleName(),
                        config.getParticleCount());
            }
        }
        if (player != null) {
            if (armor && config.isResetPlayerHealthIfArmor()) {
                HealthCompat.resetMaxHealth(player);
            }
            player.sendMessage(config.getPrefix() + config.getExpire());
        }
        runExpiryCommands(player, config);
        plugin.discordWebhook().notifyExpiry(item, player, world, config);
        return true;
    }

    /**
     * Console commands run on expiry. Commands containing {player} only
     * run when a player is known. On Folia dispatching runs on the global
     * region, where console commands belong.
     */
    private void runExpiryCommands(Player player, PluginConfig config) {
        final Runnable dispatch = () -> {
            for (String command : config.getExpiryCommands()) {
                if (player == null && command.contains("{player}")) {
                    continue;
                }
                String cmd = command.replace("{player}", player == null ? "" : player.getName());
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }
                if (!cmd.isEmpty()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }
        };
        if (SchedulerCompat.isFolia()) {
            SchedulerCompat.runGlobal(plugin, dispatch);
        } else {
            dispatch.run();
        }
    }

    /** Nearest player within the radius of the location, or null. */
    public static Player nearestPlayer(Location center, double radius) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player player : center.getWorld().getPlayers()) {
            if (!player.getWorld().equals(center.getWorld())) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(center);
            if (distance <= radius * radius && distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
