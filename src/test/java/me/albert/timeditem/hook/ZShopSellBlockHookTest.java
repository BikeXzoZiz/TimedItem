package me.albert.timeditem.hook;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * zShop (fr.maxlego08.shop.api) is never on the compile or test
 * classpath - these stubs mirror the API shapes the zShop hook probes:
 * a cancellable event whose static HandlerList and setCancelled live on
 * the base classes (inherited resolution must work), getPlayer +
 * getShopActions on the event, and ShopAction.getItemStack for the
 * extractor.
 */
class ZShopSellBlockHookTest {

    /** zShop: ShopEvent / CancelledShopEvent - shared HandlerList + Cancellable. */
    public static class ShopEventStub extends Event implements Cancellable {
        private static final HandlerList handlers = new HandlerList();
        private boolean cancelled;

        @Override
        public HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    /** zShop: ZShopSellAllEvent - player + one ShopAction per sold entry. */
    public static class SellAllEventStub extends ShopEventStub {
        public Player getPlayer() {
            return null;
        }

        public List<ShopActionStub> getShopActions() {
            return Arrays.asList(new ShopActionStub(Material.DIAMOND));
        }
    }

    /** zShop: ShopAction - the only place holding the player's real stack. */
    public static class ShopActionStub {
        private final ItemStack stack;

        ShopActionStub(Material material) {
            this.stack = new ItemStack(material);
        }

        public ItemStack getItemStack() {
            return stack;
        }
    }

    @Test
    void probesResolveZShopShapedMembers() {
        assertNotNull(HookReflection.findGetter(SellAllEventStub.class, "getplayer", Player.class));
        assertNotNull(HookReflection.findCollectionGetter(SellAllEventStub.class, "getshopactions"));
        assertNotNull(HookReflection.findGetter(ShopActionStub.class, "getitemstack", ItemStack.class));
        assertNullOnWrongType();
    }

    private void assertNullOnWrongType() {
        // getItemButton() on ZShopSellEvent returns ItemButton, not ItemStack - the
        // getter probe must ignore it (this is why the single-sell event is not hooked)
        org.junit.jupiter.api.Assertions.assertNull(
                HookReflection.findGetter(SellAllEventStub.class, "getitem", ItemStack.class));
    }

    @Test
    void cancelAndHandlerListResolveThroughInheritance() throws Exception {
        // zShop declares setCancelled on CancelledShopEvent and the static
        // getHandlerList on ShopEvent, never on the concrete event class -
        // getMethod/getHandlers must still see them through inheritance
        assertNotNull(HookReflection.findMethod(SellAllEventStub.class, "setCancelled", boolean.class));
        Method handlers = SellAllEventStub.class.getMethod("getHandlerList");
        assertNotNull(handlers.invoke(null));
    }

    @Test
    void registersAgainstInheritedHandlerList() {
        // plugin is null here: RegisteredListener stores it untouched and the
        // handler never fires in a unit test - this pins the registration path
        // (event class resolution + HandlerList lookup) for the zShop shape
        ReflectiveEventHook.Registration registration = ShopSellBlockHook.register(
                null, "zShop", SellAllEventStub.class,
                HookReflection.findGetter(SellAllEventStub.class, "getplayer", Player.class),
                event -> Collections.singletonList(new ItemStack(Material.STONE)));
        assertNotNull(registration);
        registration.unregister();
    }
}
