package me.albert.timeditem;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Resets a player's max health across server versions. The Attribute
 * constant GENERIC_MAX_HEALTH was renamed to MAX_HEALTH in 1.21.3 and
 * LivingEntity#setMaxHealth was removed later, so the call goes through
 * reflection trying both names before falling back to the legacy setter.
 */
public final class HealthCompat {

    private HealthCompat() {
    }

    public static void resetMaxHealth(Player player) {
        try {
            Class<?> attributeClass = Class.forName("org.bukkit.attribute.Attribute");
            Object attribute = resolveMaxHealth(attributeClass);
            if (attribute != null) {
                Method getAttribute = Player.class.getMethod("getAttribute", attributeClass);
                Object instance = getAttribute.invoke(player, attribute);
                if (instance != null) {
                    instance.getClass().getMethod("setBaseValue", double.class).invoke(instance, 20.0);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // fall through to the legacy setter
        }
        try {
            Player.class.getMethod("setMaxHealth", double.class).invoke(player, 20.0);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // neither API exists on this server; nothing more we can do
        }
    }

    private static Object resolveMaxHealth(Class<?> attributeClass) {
        String[] candidates = {"MAX_HEALTH", "GENERIC_MAX_HEALTH"};
        for (String name : candidates) {
            try {
                return attributeClass.getField(name).get(null);
            } catch (NoSuchFieldException ignored) {
                // try the next candidate name
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }
}
