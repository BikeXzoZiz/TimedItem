package me.albert.timeditem;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Resets a player's max health across server versions. The Attribute
 * constant GENERIC_MAX_HEALTH was renamed to MAX_HEALTH in 1.21.3 and
 * LivingEntity#setMaxHealth was removed later, so the call goes through
 * reflection trying both names before falling back to the legacy setter.
 *
 * The reset only lowers values above 20 so another plugin's max-health
 * setup is never stomped, and a player below 20 is never buffed.
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
                    Method getBase = instance.getClass().getMethod("getBaseValue");
                    double current = (Double) getBase.invoke(instance);
                    if (current <= 20.0) {
                        return; // nothing to reset
                    }
                    instance.getClass().getMethod("setBaseValue", double.class).invoke(instance, 20.0);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // fall through to the legacy setter
        }
        try {
            Method setMax = Player.class.getMethod("setMaxHealth", double.class);
            Method getMax = Player.class.getMethod("getMaxHealth");
            double current = (Double) getMax.invoke(player);
            if (current > 20.0) {
                setMax.invoke(player, 20.0);
            }
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
