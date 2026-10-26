package fake.shopkeepers;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Cross-plugin bridge the harness reads reflectively. */
public class Bridge {

    /** The stack the harness wants the next fake trade to hand out. */
    public static ItemStack nextItem;

    /** Post-event stack of every fake trade, in fire order. */
    public static final List<ItemStack> results = new ArrayList<ItemStack>();
}
