package com.badbones69.crazycrates.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fake prize event (the badbones69 PlayerPrizeEvent shape the auto-timer
 * hook probes first): the prize stack is readable and replaceable.
 */
public class PlayerPrizeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private ItemStack prize;
    private boolean cancelled;

    public PlayerPrizeEvent(ItemStack prize) {
        this(null, prize);
    }

    public PlayerPrizeEvent(Player player, ItemStack prize) {
        this.player = player;
        this.prize = prize;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItemStack() {
        return prize;
    }

    public void setItemStack(ItemStack prize) {
        this.prize = prize;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
