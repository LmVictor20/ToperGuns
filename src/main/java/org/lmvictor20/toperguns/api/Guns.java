package org.lmvictor20.toperguns.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Static accessor for the public API via Bukkit ServicesManager.
 */
public final class Guns {

    private static volatile GunsAPI cached;

    private Guns() {}

    public static GunsAPI api() {
        GunsAPI local = cached;
        if (local != null) return local;
        synchronized (Guns.class) {
            if (cached != null) return cached;
            RegisteredServiceProvider<GunsAPI> rsp = Bukkit.getServicesManager().getRegistration(GunsAPI.class);
            if (rsp != null) {
                cached = rsp.getProvider();
            }
            return cached;
        }
    }
}


