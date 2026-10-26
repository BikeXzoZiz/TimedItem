package me.albert.timeditem;

import me.albert.timeditem.api.ItemExpireEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The single expiry pipeline shared by every context: player-bound
 * events, the periodic inventory scan and ground/frame/armor-stand
 * expiry. Fires the cancellable {@link ItemExpireEvent}, records the
 * recovery entry, removes the item and runs the configured effects and
 * commands. With expiry transformation enabled (2.8+) the item instead
 * turns into its configured replacement (per-item profile key in the
 * PDC, or the configured default) - the event lets developers swap or
 * clear that replacement.
 *
 * The player is null for ground expiry: those runs hand the sound and
 * particles to the nearest player instead and skip commands containing
 * {player} (there is no player to substitute).
 */
public final class ExpiryService {

    /** Radius in which a player sees and hears a ground expiry. */
    private static final double GROUND_EFFECT_RADIUS = 20.0;

    /** yyyy-MM-dd key for the daily expiry statistics (thread safe). */
    private static final ThreadLocal<SimpleDateFormat> DAY_FORMAT =
            new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("yyyy-MM-dd");
                }
            };

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
        Transforms.Profile profile = resolveTransform(item, config);
        if (profile != null) {
            event.setReplacement(Transforms.build(plugin, profile));
        }
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
        deliverReplacement(event.getReplacement(), profile, player, effectCenter, config);
        runExpiryCommands(player, config);
        plugin.discordWebhook().notifyExpiry(item, player, world, config);
        plugin.storage().incrementExpired(DAY_FORMAT.get().format(new Date()),
                item.getType().name());
        return true;
    }

    /**
     * The transformation profile of the item: its per-item
     * "transform:<key>" tag wins, then the configured default. Returns
     * null when transformation stays off for this item.
     */
    private Transforms.Profile resolveTransform(ItemStack item, PluginConfig config) {
        if (!config.isExpiryTransformEnabled()) {
            return null;
        }
        String key = PdcCompat.readTransform(item);
        if (key == null || key.isEmpty()) {
            key = config.getExpiryTransformDefault();
        }
        return Transforms.find(config.transformProfiles(), key);
    }

    /**
     * Hands the transformation result to the player (or drops it at the
     * item's position for ground expiry) and runs the profile's extra
     * console commands. Runs on the caller's thread, which is the
     * owner's region thread on Folia - inventory access is legal here.
     */
    private void deliverReplacement(ItemStack replacement, Transforms.Profile profile,
                                    Player player, Location effectCenter, PluginConfig config) {
        if (replacement == null) {
            return;
        }
        if (player != null) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(replacement);
            for (ItemStack rest : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }
            player.sendMessage(config.getPrefix() + config.getTransformMessage()
                    .replace("{item}", plugin.hooks() == null
                            ? replacement.getType().name()
                            : plugin.hooks().describe(replacement)));
        } else if (effectCenter != null) {
            effectCenter.getWorld().dropItemNaturally(effectCenter, replacement);
        }
        List<String> commands = profile == null ? Collections.<String>emptyList()
                : profile.getCommands();
        if (!commands.isEmpty()) {
            final List<String> toRun = commands;
            final Player target = player;
            final Runnable dispatch = () -> {
                for (String command : toRun) {
                    if (target == null && command.contains("{player}")) {
                        continue;
                    }
                    String cmd = command.replace("{player}", target == null ? "" : target.getName());
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
    }

    /**
     * Console commands run on expiry. Commands containing {player} only
     * run when a player is known. On Folia dispatching runs on the global
     * region, where console commands belong.
     */
    private void runExpiryCommands(Player player, PluginConfig config) {
        final List<String> commands = config.getExpiryCommands();
        if (commands.isEmpty()) {
            return;
        }
        final Player target = player;
        final Runnable dispatch = () -> {
            for (String command : commands) {
                if (target == null && command.contains("{player}")) {
                    continue;
                }
                String cmd = command.replace("{player}", target == null ? "" : target.getName());
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
