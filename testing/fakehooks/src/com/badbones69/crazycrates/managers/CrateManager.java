package com.badbones69.crazycrates.managers;

import com.badbones69.crazycrates.api.objects.Crate;

import java.util.HashMap;
import java.util.Map;

/** Fake crate manager; the adapter finds getCrate via its "Crate" return type. */
public class CrateManager {

    private final Map<String, Crate> crates = new HashMap<String, Crate>();

    public void registerDefaults() {
        crates.put("basic", new Crate("basic"));
    }

    /** Null for unknown crate names. */
    public Crate getCrate(String name) {
        return crates.get(name);
    }
}
