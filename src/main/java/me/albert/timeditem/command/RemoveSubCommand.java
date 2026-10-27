package me.albert.timeditem.command;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.TimeLore;
import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.storage.AuditRecord;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** /ti remove - strips the timer from the held item. */
public final class RemoveSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;

    public RemoveSubCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        Player player = CommandSupport.requirePlayer(plugin, sender);
        if (player == null) {
            return;
        }
        PluginConfig config = plugin.pluginConfig();
        ItemStack item = CommandSupport.mainHand(plugin, player);
        if (item == null) {
            return;
        }
        TimeLore.removeTimer(item, config);
        player.sendMessage(config.getPrefix() + config.getItemCleared());
        CommandSupport.audit(plugin, player, AuditRecord.REMOVE, player.getName(), item, null);
    }
}
