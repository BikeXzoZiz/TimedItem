package me.albert.timeditem.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Called after a "lifetime" item is used for the first time and its
 * concrete expiry date has been stamped (the timer started counting).
 */
public class ItemTimerStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemStack item;
    private final Player player;

    public ItemTimerStartEvent(ItemStack item, @Nullable Player player) {
        this.item = item;
        this.player = player;
    }

    public ItemStack getItem() {
        return item;
    }

    /** The player who started the timer by using the item, or null when started by an API call or config. */
    @Nullable
    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
