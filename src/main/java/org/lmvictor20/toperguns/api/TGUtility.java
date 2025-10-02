package org.lmvictor20.toperguns.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.lmvictor20.toperguns.ToperGunsPlugin;
import org.lmvictor20.toperguns.weapon.WeaponManager;

public class TGUtility {

    private final ToperGunsPlugin plugin;

    public TGUtility(ToperGunsPlugin plugin) {
        this.plugin = plugin;
    }

    public void giveWeapon(Player player, String weaponName, int amount) {
        if (player == null || weaponName == null) return;
        WeaponManager wm = plugin.getWeaponManager();
        ItemStack item = wm.generateWeapon(weaponName);
        if (item == null) return;
        item.setAmount(Math.max(1, amount));
        player.getInventory().addItem(item);
    }

    public ItemStack generateWeapon(String weaponName) {
        return plugin.getWeaponManager().generateWeapon(weaponName);
    }

    public void generateExplosion(Player player, Location loc, String weaponTitle) {
        if (loc == null) return;
        loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 2.0f, false, false);
        // Event mirroring could be fired here later
    }

    public void spawnMine(Player player, Location loc, String weaponTitle) {
        if (loc == null) return;
        TNTPrimed tnt = loc.getWorld().spawn(loc, TNTPrimed.class);
        tnt.setFuseTicks(200);
        tnt.setIsIncendiary(false);
        tnt.setYield(2.0f);
    }

    public void setProjectile(Player player, Projectile proj, String weaponTitle) {
        if (proj == null) return;
        // Basic velocity tweak to represent weapon behavior
        Vector v = proj.getVelocity();
        proj.setVelocity(v.multiply(1.2));
    }

    public String getWeaponTitle(ItemStack item) {
        return plugin.getWeaponManager().getWeaponTitle(item);
    }

    public String getWeaponTitle(Projectile proj) {
        // In a simple version we cannot identify projectiles precisely, return null
        return null;
    }

    public String getWeaponTitle(TNTPrimed tnt) {
        // In a simple version we cannot identify TNT owner/weapon, return null
        return null;
    }
}


