package com.ssomar.executableitems.executableitems.manager;

import com.ssomar.executableitems.executableitems.ExecutableItemsAPI;
import com.ssomar.executableitems.executableitems.ExecutableItemsAPI.ExecutableItem;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/** Fake manager with the Optional-returning overloads of current versions. */
public class ExecutableItemsManager {

    public Optional<ExecutableItem> getExecutableItem(ItemStack item) {
        String id = ExecutableItemsAPI.idOf(item);
        return id == null ? Optional.<ExecutableItem>empty()
                : Optional.of(new ExecutableItem(id));
    }

    public Optional<ExecutableItem> getExecutableItem(String id) {
        ItemStack stack = ExecutableItemsAPI.itemOf(id);
        return stack == null ? Optional.<ExecutableItem>empty()
                : Optional.of(new ExecutableItem(id));
    }
}
