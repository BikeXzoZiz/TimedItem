package com.nisovin.shopkeepers.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fake ShopkeeperTradeEvent with the shape of Shopkeepers 2.19+:
 * the result item can be read and replaced through
 * getItemStack()/setItemStack().
 */
public class ShopkeeperTradeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private ItemStack result;
    private boolean cancelled;

    public ShopkeeperTradeEvent(ItemStack result) {
        this(null, result);
    }

    public ShopkeeperTradeEvent(Player player, ItemStack result) {
        this.player = player;
        this.result = result;
    }

    /** The trading player, or null when the trade has no client (fake console trades). */
    public Player getPlayer() {
        return player;
    }

    public ItemStack getItemStack() {
        return result;
    }

    public void setItemStack(ItemStack result) {
        this.result = result;
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
