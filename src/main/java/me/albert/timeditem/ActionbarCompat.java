package me.albert.timeditem;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Sends actionbar text via the legacy Spigot API. On a server where the
 * bungee-chat actionbar path no longer exists, the first failure disables
 * the sender, logs one warning instead of spamming errors, and callers
 * can skip scheduling via {@link #isBroken()}.
 */
public final class ActionbarCompat {

    private static volatile boolean broken;

    private ActionbarCompat() {
    }

    /** True once the actionbar path failed hard; callers can skip scheduling. */
    public static boolean isBroken() {
        return broken;
    }

    public static void send(Player player, String legacyText) {
        if (broken) {
            return;
        }
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(legacyText));
        } catch (Throwable t) {
            broken = true;
            Logger logger = DebugLog.logger();
            if (logger != null) {
                logger.warning("Actionbar display disabled after a send failure: " + t);
            }
        }
    }
}
