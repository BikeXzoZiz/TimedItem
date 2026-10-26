package me.albert.timeditem.harness;

import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the scenarios one at a time (each in its own offset area),
 * prints a machine-parsable verdict line per scenario and a summary at
 * the end. The matrix runner greps these lines:
 *
 * <pre>
 * [TITEST] RUN  &lt;name&gt;
 * [TITEST] PASS &lt;name&gt; - &lt;detail&gt;
 * [TITEST] FAIL &lt;name&gt; - &lt;detail&gt;
 * [TITEST] SKIP &lt;name&gt; - &lt;reason&gt;
 * [TITEST] SUITE COMPLETE passed=x failed=y skipped=z total=n
 * </pre>
 */
public final class Suite implements CommandExecutor, TabCompleter {

    /** All scenarios in execution order. */
    static Scenario[] all(TimedItemPlugin plugin) {
        return new Scenario[]{
                new BootScenario(),
                new LoreRoundtripScenario(),
                new ApiFacadeScenario(),
                new GroundExpiryScenario(),
                new TransformScenario(),
                new ContainerScanScenario(),
                new HologramScenario(),
                new FoliaGatesScenario(),
                new HooksStubScenario(),
                new AutoTimerScenario()
        };
    }

    private final HarnessPlugin harness;
    private final Map<String, Scenario> byName = new LinkedHashMap<String, Scenario>();
    private Scenario[] queue;
    private Context ctx;
    private int passed;
    private int failed;
    private int skipped;

    public Suite(HarnessPlugin harness) {
        this.harness = harness;
        for (Scenario scenario : all(harness.timedItem())) {
            byName.put(scenario.name(), scenario);
        }
    }

    String[] names() {
        return byName.keySet().toArray(new String[0]);
    }

    boolean busy() {
        return queue != null;
    }

    void run(CommandSender sender, String what) {
        if (busy()) {
            sender.sendMessage("A suite is already running.");
            return;
        }
        passed = 0;
        failed = 0;
        skipped = 0;
        if (what.equalsIgnoreCase("all")) {
            queue = all(harness.timedItem());
        } else {
            Scenario scenario = byName.get(what.toLowerCase());
            if (scenario == null) {
                sender.sendMessage("Unknown scenario: " + what + " (try /titest list)");
                return;
            }
            queue = new Scenario[]{scenario};
        }
        harness.getLogger().info("[TITEST] SUITE START platforms=" + platformName()
                + " scenarios=" + queue.length);
        // vanilla forceload: guarantees fully ticking chunks around spawn
        // (no players are online, so ordinary spawn chunks stay lazy).
        // Best effort - Folia refuses world commands from the global
        // region, and there the per-entity tracking covers us instead.
        try {
            Location spawn = Context.defaultBase();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "forceload add "
                    + (spawn.getBlockX() - 96) + " " + (spawn.getBlockZ() - 96)
                    + " " + (spawn.getBlockX() + 96) + " " + (spawn.getBlockZ() + 96));
        } catch (Throwable t) {
            harness.getLogger().info("[TITEST] forceload skipped (" + t.getClass().getSimpleName() + ")");
        }
        index = 0;
        F.later(harness.timedItem(), null, 40L, this::startNext);
    }

    private int index;

    private String platformName() {
        return F.isFolia() ? "folia" : Bukkit.getName().toLowerCase() + "/" + Bukkit.getBukkitVersion();
    }

    private void startNext() {
        if (queue == null || index >= queue.length) {
            complete();
            return;
        }
        final Scenario scenario = queue[index];
        String skip = scenario.skipReason(harness.timedItem());
        if (skip != null) {
            skipped++;
            harness.getLogger().info("[TITEST] SKIP " + scenario.name() + " - " + skip);
            index++;
            F.later(harness.timedItem(), null, 5L, this::startNext);
            return;
        }
        // every scenario gets its own small lane around spawn
        // (offsets stay inside the spawn-chunk radius)
        Location base = Context.defaultBase().add(index * 8.0, 0, index * 8.0);
        if (!F.isFolia()) {
            // Folia: reading the world height from the global region is
            // illegal - the spawn Y is already a valid ground position
            base.setY(Math.max(base.getWorld().getHighestBlockYAt(base) + 1, base.getY()));
        }
        // no players are online, so chunks unload behind our backs; pin
        // the scenario's chunk (best effort - keeps entities ticking).
        // Reflection: World.setForceLoaded is 1.14+, compile target is 1.13.
        try {
            base.getWorld().getClass().getMethod("setForceLoaded", int.class, int.class,
                    boolean.class).invoke(base.getWorld(),
                    base.getBlockX() >> 4, base.getBlockZ() >> 4, true);
        } catch (Throwable ignored) {
            // 1.13 servers or Folia restrictions - the lane will tell us
        }
        ctx = new Context(harness.timedItem(), base, harness.getLogger());
        harness.getLogger().info("[TITEST] RUN " + scenario.name());
        try {
            scenario.start(ctx);
        } catch (Throwable t) {
            ctx.finish(false, "start() threw " + t);
        }
        if (ctx.finished()) {
            conclude(scenario);
            return;
        }
        armPoll(scenario);
    }

    private void armPoll(final Scenario scenario) {
        F.later(harness.timedItem(), ctx.base(), 20L, new Runnable() {
            @Override
            public void run() {
                if (ctx.finished()) {
                    conclude(scenario);
                    return;
                }
                if (ctx.elapsedSeconds() >= scenario.timeoutSeconds()) {
                    ctx.finish(false, "timeout after " + scenario.timeoutSeconds() + "s");
                    conclude(scenario);
                    return;
                }
                try {
                    scenario.poll(ctx);
                } catch (Throwable t) {
                    ctx.finish(false, "poll() threw " + t);
                }
                if (!ctx.finished()) {
                    armPoll(scenario);
                    return;
                }
                conclude(scenario);
            }
        });
    }

    private void conclude(final Scenario scenario) {
        final Context done = ctx;
        ctx = null;
        done.runCleanup();
        boolean pass = "PASS".equals(done.result());
        if (pass) {
            passed++;
        } else {
            failed++;
        }
        harness.getLogger().info("[TITEST] " + done.result() + " " + scenario.name()
                + " - " + done.detail());
        index++;
        F.later(harness.timedItem(), done.base(), 10L, this::startNext);
    }

    private void complete() {
        queue = null;
        int total = passed + failed + skipped;
        harness.getLogger().info("[TITEST] SUITE COMPLETE passed=" + passed
                + " failed=" + failed + " skipped=" + skipped + " total=" + total);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("Scenarios: " + String.join(", ", names()));
            return true;
        }
        if (args[0].equalsIgnoreCase("run") && args.length >= 2) {
            run(sender, args[1]);
            return true;
        }
        sender.sendMessage("Usage: /titest run <scenario|all> | /titest list");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label,
                                      String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            out.addAll(Arrays.asList("run", "list"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("run")) {
            out.addAll(byName.keySet());
            out.add("all");
        }
        return out;
    }
}
