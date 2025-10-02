package org.lmvictor20.toperguns.api.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.lmvictor20.toperguns.ToperGunsPlugin;
import org.lmvictor20.toperguns.api.GunsAPI;
import org.lmvictor20.toperguns.api.WeaponDefinition;
import org.lmvictor20.toperguns.weapon.WeaponManager;

import java.util.*;

public class GunsAPIImpl implements GunsAPI {

    public static final String META_WEAPON_ID = "gun-weapon-id";
    public static final String META_DAMAGE = "gun-damage";
    public static final String META_FORCE = "gun-force";
    private static final String LORE_MARKER_PREFIX = "§8[TG:id=";
    private static final String LORE_MARKER_SUFFIX = "]";

    private final ToperGunsPlugin plugin;

    public GunsAPIImpl(ToperGunsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Collection<String> getWeaponIds() {
        Set<String> ids = new LinkedHashSet<String>();
        try {
            org.bukkit.configuration.file.FileConfiguration cfg = new org.bukkit.configuration.file.YamlConfiguration();
            java.io.File f = new java.io.File(plugin.getDataFolder(), "weapons.yml");
            if (f.exists()) {
                cfg.load(f);
                ids.addAll(cfg.getKeys(false));
            }
        } catch (Exception ignored) {}
        return Collections.unmodifiableCollection(ids);
    }

    @Override
    public boolean hasWeapon(String weaponId) {
        return getWeapon(weaponId).isPresent();
    }

    @Override
    public Optional<WeaponDefinition> getWeapon(String weaponId) {
        if (weaponId == null) return Optional.empty();
        String canonical = resolveAlias(weaponId);
        org.lmvictor20.toperguns.weapon.WeaponDefinition def = plugin.getWeaponManager().getDefinition(canonical);
        if (def == null) return Optional.empty();
        double kb = def.abilityKnockback;
        return Optional.of(new WeaponDefinition(def.title, def.display, def.damage, kb));
    }

    @Override
    public ItemStack createWeaponItem(String weaponId, int amount) {
        String canonical = resolveAlias(weaponId);
        ItemStack it = plugin.getWeaponManager().generateWeapon(canonical);
        if (it == null) return null;
        it.setAmount(Math.max(1, amount));
        embedIdIntoItem(it, canonical);
        return it;
    }

    @Override
    public boolean isWeaponItem(ItemStack stack) {
        return getWeaponId(stack).isPresent();
    }

    @Override
    public Optional<String> getWeaponId(ItemStack stack) {
        if (stack == null) return Optional.empty();
        // Prefer embedded lore marker
        String fromLore = extractIdFromItem(stack);
        if (fromLore != null) return Optional.of(fromLore);
        // Fallback to display-name recognition
        String title = plugin.getWeaponManager().getWeaponTitle(stack);
        return title == null ? Optional.empty() : Optional.of(title);
    }

    @Override
    public Optional<String> getWeaponId(Projectile projectile) {
        if (projectile == null) return Optional.empty();
        // Read metadata first
        if (projectile.hasMetadata(META_WEAPON_ID)) {
            List<MetadataValue> vals = projectile.getMetadata(META_WEAPON_ID);
            for (MetadataValue v : vals) {
                try { String s = v.asString(); if (s != null && !s.isEmpty()) return Optional.of(s); } catch (Throwable ignored) {}
            }
        }
        // Fallback to customName "TG|title|damage"
        try {
            String n = projectile.getCustomName();
            if (n != null && n.startsWith("TG|")) {
                String[] parts = n.split("\\|");
                if (parts.length >= 2) return Optional.of(parts[1]);
            }
        } catch (Throwable ignored) {}
        return Optional.empty();
    }

    private void embedIdIntoItem(ItemStack item, String id) {
        if (item == null || id == null) return;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        java.util.List<String> lore = meta.hasLore() ? new java.util.ArrayList<String>(meta.getLore()) : new java.util.ArrayList<String>();
        String marker = LORE_MARKER_PREFIX + id + LORE_MARKER_SUFFIX;
        // Ensure only one marker
        boolean present = false;
        for (String line : lore) { if (line != null && line.contains("[TG:id=")) { present = true; break; } }
        if (!present) lore.add(marker);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private String extractIdFromItem(ItemStack item) {
        try {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasLore()) return null;
            for (String line : meta.getLore()) {
                if (line == null) continue;
                int idx = line.indexOf("[TG:id=");
                if (idx >= 0) {
                    int end = line.indexOf("]", idx);
                    if (end > idx) {
                        String inner = line.substring(idx + 7, end);
                        if (!inner.isEmpty()) return inner;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private String resolveAlias(String id) {
        if (id == null) return null;
        try {
            org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
            if (cfg != null && cfg.isConfigurationSection("aliases")) {
                org.bukkit.configuration.ConfigurationSection s = cfg.getConfigurationSection("aliases");
                if (s != null && s.isString(id)) {
                    String mapped = s.getString(id);
                    if (mapped != null && !mapped.isEmpty()) return mapped;
                }
            }
        } catch (Throwable ignored) {}
        return id;
    }
}


