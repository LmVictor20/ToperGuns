package org.lmvictor20.toperguns.event;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class WeaponEvents {

    public static abstract class Base extends Event implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();
        private boolean cancelled;
        private final Player player;
        private final String weaponTitle;

        protected Base(Player player, String weaponTitle) {
            this.player = player;
            this.weaponTitle = weaponTitle;
        }

        public Player getPlayer() { return player; }
        public String getWeaponTitle() { return weaponTitle; }
        @Override public boolean isCancelled() { return cancelled; }
        @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
        @Override public HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    public static class WeaponPrepareShootEvent extends Base {
        public WeaponPrepareShootEvent(Player player, String weaponTitle) { super(player, weaponTitle); }
    }

    public static class WeaponPreShootEvent extends Base {
        private double bulletSpread;
        public WeaponPreShootEvent(Player player, String weaponTitle, double bulletSpread) {
            super(player, weaponTitle);
            this.bulletSpread = bulletSpread;
        }
        public double getBulletSpread() { return bulletSpread; }
        public void setBulletSpread(double bulletSpread) { this.bulletSpread = bulletSpread; }
    }

    public static class WeaponShootEvent extends Base {
        private final Entity projectile;
        public WeaponShootEvent(Player player, String weaponTitle, Entity projectile) {
            super(player, weaponTitle);
            this.projectile = projectile;
        }
        public Entity getProjectile() { return projectile; }
    }

    public static class WeaponDamageEntityEvent extends Base {
        private final Entity victim;
        private final Entity damager;
        private double damage;
        public WeaponDamageEntityEvent(Player player, String weaponTitle, Entity victim, Entity damager, double damage) {
            super(player, weaponTitle);
            this.victim = victim;
            this.damager = damager;
            this.damage = damage;
        }
        public Entity getVictim() { return victim; }
        public Entity getDamager() { return damager; }
        public double getDamage() { return damage; }
        public void setDamage(double damage) { this.damage = damage; }
        public boolean isHeadshot() { return false; }
        public boolean isCritical() { return false; }
        public boolean isBackstab() { return false; }
    }

    public static class WeaponExplodeEvent extends Base {
        private final Location location;
        public WeaponExplodeEvent(Player player, String weaponTitle, Location location) {
            super(player, weaponTitle);
            this.location = location;
        }
        public Location getLocation() { return location; }
        public boolean isSplit() { return false; }
        public boolean isAirstrike() { return false; }
    }

    public static class WeaponReloadEvent extends Base {
        private double reloadSpeed;
        public WeaponReloadEvent(Player player, String weaponTitle, double reloadSpeed) {
            super(player, weaponTitle);
            this.reloadSpeed = reloadSpeed;
        }
        public double getReloadSpeed() { return reloadSpeed; }
        public void setReloadSpeed(double reloadSpeed) { this.reloadSpeed = reloadSpeed; }
        public String[] getSounds() { return new String[0]; }
        public void setSounds(String[] sounds) { }
    }

    public static class WeaponReloadCompleteEvent extends Base {
        public WeaponReloadCompleteEvent(Player player, String weaponTitle) { super(player, weaponTitle); }
    }

    public static class WeaponHitBlockEvent extends Base {
        private final Entity projectile;
        public WeaponHitBlockEvent(Player player, String weaponTitle, Entity projectile) {
            super(player, weaponTitle);
            this.projectile = projectile;
        }
        public Entity getProjectile() { return projectile; }
        public org.bukkit.block.Block getBlock() { return null; }
        public org.bukkit.block.Block getAirBlock() { return null; }
    }

    public static class WeaponScopeEvent extends Base {
        private final boolean zoomIn;
        public WeaponScopeEvent(Player player, String weaponTitle, boolean zoomIn) {
            super(player, weaponTitle);
            this.zoomIn = zoomIn;
        }
        public boolean isZoomIn() { return zoomIn; }
    }

    public static class WeaponPlaceMineEvent extends Base {
        private final Entity mine;
        public WeaponPlaceMineEvent(Player player, String weaponTitle, Entity mine) {
            super(player, weaponTitle);
            this.mine = mine;
        }
        public Entity getMine() { return mine; }
    }

    public static class WeaponTriggerEvent extends Base {
        private final LivingEntity victim;
        public WeaponTriggerEvent(Player player, String weaponTitle, LivingEntity victim) {
            super(player, weaponTitle);
            this.victim = victim;
        }
        public LivingEntity getVictim() { return victim; }
    }

    public static class WeaponFireRateEvent extends Base {
        private int fireRate;
        private final ItemStack itemStack;
        public WeaponFireRateEvent(Player player, String weaponTitle, int fireRate, ItemStack itemStack) {
            super(player, weaponTitle);
            this.fireRate = fireRate;
            this.itemStack = itemStack;
        }
        public int getFireRate() { return fireRate; }
        public void setFireRate(int fireRate) { this.fireRate = fireRate; }
        public ItemStack getItemStack() { return itemStack; }
    }
}


