package org.lmvictor20.toperguns.api.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GunsDamageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled;
    private final Player shooter;
    private final Entity victim;
    private final Projectile projectile; // nullable
    private final String weaponId;
    private double damage;
    private double knockbackForce;

    public GunsDamageEvent(Player shooter, Entity victim, Projectile projectile, String weaponId, double damage, double knockbackForce) {
        this.shooter = shooter;
        this.victim = victim;
        this.projectile = projectile;
        this.weaponId = weaponId;
        this.damage = damage;
        this.knockbackForce = knockbackForce;
    }

    public Player getShooter() { return shooter; }
    public Entity getVictim() { return victim; }
    public Projectile getProjectile() { return projectile; }
    public String getWeaponId() { return weaponId; }
    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }
    public double getKnockbackForce() { return knockbackForce; }
    public void setKnockbackForce(double knockbackForce) { this.knockbackForce = knockbackForce; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}


