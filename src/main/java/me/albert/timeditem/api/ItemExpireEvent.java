package me.albert.timeditem.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Called before a timed item is removed because its timer ran out.
 * Cancelling keeps the item in place (the check will fire again on the
 * next scan/click, so cancelling permanently requires cancelling every
 * time).
 *
 * The player is null when the item expires outside an inventory, for
 * example on the ground or in an item frame. The replacement item
 * (expiry transformation, 2.8+) can be swapped or cleared with
 * {@link #setReplacement(ItemStack)} - null means the item just
 * disappears.
 */
public class ItemExpireEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemStack item;
    private final Player player;
    private ItemStack replacement;
    private boolean cancelled;

    public ItemExpireEvent(ItemStack item, @Nullable Player player) {
        this.item = item;
        this.player = player;
    }

    public ItemStack getItem() {
        return item;
    }

    /** The player losing the item, or null when it expires outside an inventory. */
    @Nullable
    public Player getPlayer() {
        return player;
    }

    /**
     * The item the expiring item turns into (expiry transformation), or
     * null when it should simply disappear.
     */
    @Nullable
    public ItemStack getReplacement() {
        return replacement;
    }

    /** Overrides the transformation result; null disables the transformation. */
    public void setReplacement(@Nullable ItemStack replacement) {
        this.replacement = replacement;
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
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
