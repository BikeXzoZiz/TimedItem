package me.albert.timeditem.hook;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins the reflection helpers every hook depends on: missing classes,
 * methods and fields must degrade to null (never throw), so a mismatched
 * foreign plugin version only disables the hook.
 */
class HookReflectionTest {

    /** Helper for the collection-getter and static-field probes. */
    public static class Probe {
        private static final String SECRET = "hidden";
        public static final String PUBLIC = "visible";

        public List<String> getValues() {
            return Arrays.asList("a", "b");
        }

        public String getBogus() {
            return "not a collection";
        }

        public String boom() {
            throw new IllegalStateException("boom");
        }
    }

    @Test
    void findClassPicksTheFirstExistingCandidate() {
        assertEquals(String.class, HookReflection.findClass("java.lang.String"));
        assertEquals(StringBuilder.class,
                HookReflection.findClass("java.lang.DoesNotExist", "java.lang.StringBuilder"));
        assertNull(HookReflection.findClass("java.lang.DoesNotExist", "also.missing.Fake"));
        assertNull(HookReflection.findClass());
    }

    @Test
    void findMethodNeedsAnExactSignature() {
        Method substring = HookReflection.findMethod(String.class, "substring", int.class);
        assertNotNull(substring);
        assertNull(HookReflection.findMethod(String.class, "substring", String.class));
        assertNull(HookReflection.findMethod(String.class, "noSuchMethod", int.class));
    }

    @Test
    void findOverloadMatchesBySingleParameterType() {
        assertNotNull(HookReflection.findOverload(String.class, "valueOf", int.class));
        // String has valueOf overloads for primitives and Object, but not for String itself
        assertNull(HookReflection.findOverload(String.class, "valueOf", String.class));
    }

    @Test
    void findAnyMethodMatchesByNameAndArityOnly() {
        assertNotNull(HookReflection.findAnyMethod(String.class, "valueOf", 1));
        assertNull(HookReflection.findAnyMethod(String.class, "nope", 1));
    }

    @Test
    void findByReturnMatchesReturnTypeSimpleName() {
        // Boolean.valueOf(String) returns "Boolean"; parseBoolean returns primitive "boolean"
        Method method = HookReflection.findByReturn(Boolean.class, "Boolean", 1);
        assertNotNull(method);
        assertEquals("valueOf", method.getName());
        assertNull(HookReflection.findByReturn(Boolean.class, "NoSuchType", 1));
    }

    @Test
    void staticFieldReadsPublicAndPrivateFields() {
        assertEquals("visible", HookReflection.staticField(Probe.class, "PUBLIC"));
        assertEquals("hidden", HookReflection.staticField(Probe.class, "MISSING", "SECRET"));
        assertNull(HookReflection.staticField(Probe.class, "MISSING"));
    }

    @Test
    void findGetterMatchesNameFragmentAndReturnType() {
        assertNotNull(HookReflection.findGetter(String.class, "leng", int.class));
        assertNull(HookReflection.findGetter(String.class, "zzz", int.class));
        assertNull(HookReflection.findGetter(Probe.class, "value", String.class)); // wrong arity
    }

    @Test
    void findCollectionGetterNeedsAnIterableReturn() {
        Method values = HookReflection.findCollectionGetter(Probe.class, "values");
        assertNotNull(values);
        assertNull(HookReflection.findCollectionGetter(Probe.class, "bogus"));
    }

    @Test
    void invokeSwallowsFailuresAsNull() throws Exception {
        Method parseInt = Integer.class.getMethod("parseInt", String.class);
        assertEquals(42, invokeInt(HookReflection.invoke(parseInt, null, "42")));
        assertNull(HookReflection.invoke(parseInt, null, "not-a-number"));
        Method boom = Probe.class.getMethod("boom");
        assertNull(HookReflection.invoke(boom, new Probe()));
    }

    private static int invokeInt(Object boxed) {
        return (Integer) boxed;
    }
}
