package org.lmvictor20.toperguns.api;

import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;

/**
 * Minimal public API for ToperGuns allowing external plugins to list weapons,
 * create weapon items, and inspect items/projectiles for weapon identity.
 */
public interface GunsAPI {

    Collection<String> getWeaponIds();

    boolean hasWeapon(String weaponId);

    Optional<WeaponDefinition> getWeapon(String weaponId);

    ItemStack createWeaponItem(String weaponId, int amount);

    boolean isWeaponItem(ItemStack stack);

    Optional<String> getWeaponId(ItemStack stack);

    Optional<String> getWeaponId(Projectile projectile);
}


