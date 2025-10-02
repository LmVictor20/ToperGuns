package org.lmvictor20.toperguns.api;

/**
 * Immutable minimal DTO exposed via the public API. Values map to internal
 * weapon definition but only include stable fields needed by external plugins.
 */
public final class WeaponDefinition {

    private final String id;
    private final String displayName;
    private final double baseDamage;
    private final double knockbackForce;

    public WeaponDefinition(String id, String displayName, double baseDamage, double knockbackForce) {
        this.id = id;
        this.displayName = displayName;
        this.baseDamage = baseDamage;
        this.knockbackForce = knockbackForce;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public double baseDamage() { return baseDamage; }
    public double knockbackForce() { return knockbackForce; }
}


