package me.albert.timeditem.hook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Small reflection helpers shared by every hook. The integration hooks
 * never compile against a foreign API: the plugins they target ship
 * Java 17 bytecode (and are premium), while TimedItem keeps its Java 8
 * single-jar promise. Every helper swallows any Throwable - a mismatched
 * API version degrades the hook to "not available" instead of crashing.
 */
final class HookReflection {

    private HookReflection() {
    }

    /** First class name that loads, or null when none exists. */
    static Class<?> findClass(String... candidates) {
        for (String name : candidates) {
            try {
                return Class.forName(name);
            } catch (Throwable ignored) {
                // try the next candidate (older/newer package or class name)
            }
        }
        return null;
    }

    /** Public declared method with an exact signature, or null. */
    static Method findMethod(Class<?> owner, String name, Class<?>... params) {
        try {
            return owner.getMethod(name, params);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** The overload of {@code name} taking exactly one {@code param}, or null. */
    static Method findOverload(Class<?> owner, String name, Class<?> param) {
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(name)
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0] == param) {
                return method;
            }
        }
        return null;
    }

    /**
     * First public method with the given name and parameter count,
     * whatever its parameter types - used for builder calls whose
     * signatures changed between plugin versions (e.g. varargs or an
     * added option parameter).
     */
    static Method findAnyMethod(Class<?> owner, String name, int params) {
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == params) {
                return method;
            }
        }
        return null;
    }

    /**
     * First public method returning a type whose simple name matches
     * {@code returnTypeName} and taking exactly {@code params} String
     * parameters - lets hooks survive renames like getCrateByName ->
     * getCrate by looking at the return type instead of the name.
     */
    static Method findByReturn(Class<?> owner, String returnTypeName, int stringParams) {
        for (Method method : owner.getMethods()) {
            if (method.getParameterCount() != stringParams) {
                continue;
            }
            boolean allStrings = true;
            for (Class<?> type : method.getParameterTypes()) {
                if (type != String.class) {
                    allStrings = false;
                    break;
                }
            }
            if (!allStrings) {
                continue;
            }
            Class<?> returns = method.getReturnType();
            if (returns.getSimpleName().equals(returnTypeName)) {
                return method;
            }
        }
        return null;
    }

    /** Static field value (public or not), or null. */
    static Object staticField(Class<?> owner, String... names) {
        for (String name : names) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(null);
            } catch (Throwable ignored) {
                // try the next name
            }
        }
        return null;
    }

    /** Reads an object's ItemStack or String field via its no-arg methods. */
    static Method findGetter(Class<?> owner, String contains, Class<?> returns) {
        for (Method method : owner.getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() != returns) {
                continue;
            }
            String name = method.getName().toLowerCase(java.util.Locale.ROOT);
            if (name.contains(contains)) {
                return method;
            }
        }
        return null;
    }

    /**
     * No-arg method returning any collection whose name contains the
     * given fragment - matches Kotlin getValues() as well as a plain
     * values() accessor on plugin registries.
     */
    static Method findCollectionGetter(Class<?> owner, String contains) {
        for (Method method : owner.getMethods()) {
            if (method.getParameterCount() != 0
                    || !Iterable.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            String name = method.getName().toLowerCase(java.util.Locale.ROOT);
            if (name.contains(contains)) {
                return method;
            }
        }
        return null;
    }

    /** Invokes a method, converting any failure into null. */
    static Object invoke(Method method, Object owner, Object... args) {
        try {
            return method.invoke(owner, args);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
