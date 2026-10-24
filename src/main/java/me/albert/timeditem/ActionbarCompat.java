package me.albert.timeditem;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/**
 * Sends actionbar text via the legacy Spigot API. On a server where the
 * bungee-chat actionbar path no longer exists, the first failure disables
 * the sender instead of spamming errors (ActionbarTask also guards calls).
 */
public final class ActionbarCompat {

    private static boolean broken;

    private ActionbarCompat() {
    }

    public static void send(Player player, String legacyText) {
        if (broken) {
            return;
        }
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(legacyText));
        } catch (Throwable t) {
            broken = true;
        }
    }
}
