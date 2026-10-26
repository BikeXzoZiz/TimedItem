package fake.crazycrates;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Cross-plugin bridge the harness reads reflectively. */
public class Bridge {

    /** The stack the harness wants the next fake prize event to hand out. */
    public static ItemStack nextItem;

    /** Post-event stack of every fake prize, in fire order. */
    public static final List<ItemStack> results = new ArrayList<ItemStack>();
}
