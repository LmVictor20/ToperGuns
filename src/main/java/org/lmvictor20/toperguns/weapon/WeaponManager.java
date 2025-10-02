package org.lmvictor20.toperguns.weapon;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lmvictor20.toperguns.ToperGunsPlugin;

import java.io.File;
import java.util.*;

public class WeaponManager {

    private final ToperGunsPlugin plugin;
    private File weaponsFile;
    private FileConfiguration weaponsConfig;
    private final Map<String, ItemStack> titleToItem = new HashMap<String, ItemStack>();
    private final Map<String, WeaponDefinition> titleToDef = new HashMap<String, WeaponDefinition>();

    public WeaponManager(ToperGunsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        if (weaponsFile == null) {
            weaponsFile = new File(plugin.getDataFolder(), "weapons.yml");
        }
        if (!weaponsFile.exists()) {
            plugin.saveResource("weapons.yml", false);
        }
        weaponsConfig = YamlConfiguration.loadConfiguration(weaponsFile);
        loadWeapons();
    }

    private void loadWeapons() {
        titleToItem.clear();
        titleToDef.clear();
        // Parse legacy simple format section if present
        ConfigurationSection legacyRoot = weaponsConfig.getConfigurationSection("weapons");
        if (legacyRoot != null && !legacyRoot.getKeys(false).isEmpty()) {
            parseLegacyWeapons(legacyRoot);
        }
        // Parse CrackShot-like format from the main weapons.yml
        parseCrackShotStyle(weaponsConfig);

        // Parse additional files from dataFolder/weapons/*.yml if present
        File extraDir = new File(plugin.getDataFolder(), "weapons");
        if (extraDir.isDirectory()) {
            File[] files = extraDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".yml")) {
                        try {
                            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                            parseCrackShotStyle(cfg);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    private void parseLegacyWeapons(ConfigurationSection root) {
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            String title = key;
            String materialName = s.getString("material", "IRON_HOE");
            Material material = Material.matchMaterial(materialName);
            if (material == null) material = Material.IRON_HOE;
            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String display = ChatColor.translateAlternateColorCodes('&', s.getString("display", title));
                meta.setDisplayName(display);
                List<String> lore = new ArrayList<String>();
                for (String line : s.getStringList("lore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                if (!lore.isEmpty()) meta.setLore(lore);
                item.setItemMeta(meta);
            }
            titleToItem.put(title.toLowerCase(Locale.ROOT), item);

            String projectile = s.getString("projectile", "SNOWBALL");
            double damage = s.getDouble("damage", 4.0);
            double speed = s.getDouble("speed", 1.2);
            double spread = s.getDouble("spread", 2.0);
            long cooldown = s.getLong("cooldown_ms", 250);

            WeaponDefinition def = new WeaponDefinition(title, material, s.getString("display", title),
                    s.getStringList("lore"), projectile, damage, speed, spread, cooldown, "CHICKEN_EGG_POP");
            titleToDef.put(title.toLowerCase(Locale.ROOT), def);
        }
    }

    private void parseCrackShotStyle(ConfigurationSection root) {
        for (String weaponKey : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(weaponKey);
            if (s == null) continue;
            String title = weaponKey;

            ConfigurationSection itemInfo = s.getConfigurationSection("Item_Information");
            String materialName = itemInfo != null ? itemInfo.getString("Item_Type", "IRON_HOE") : "IRON_HOE";
            Material material = parseFlexibleMaterial(materialName);
            if (material == null) material = Material.IRON_HOE;
            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            String displayValue = itemInfo != null ? itemInfo.getString("Item_Name", title) : title;
            List<String> loreValue;
            if (itemInfo != null) {
                if (itemInfo.isList("Item_Lore")) {
                    loreValue = itemInfo.getStringList("Item_Lore");
                } else {
                    String loreLine = itemInfo.getString("Item_Lore", "");
                    if (loreLine == null) loreLine = "";
                    loreValue = new ArrayList<String>();
                    if (!loreLine.isEmpty()) {
                        for (String part : loreLine.split("\\|")) {
                            loreValue.add(part);
                        }
                    }
                }
            } else {
                loreValue = Collections.<String>emptyList();
            }
            if (meta != null) {
                String display = ChatColor.translateAlternateColorCodes('&', displayValue);
                meta.setDisplayName(display);
                List<String> lore = new ArrayList<String>();
                for (String line : loreValue) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                if (!lore.isEmpty()) meta.setLore(lore);
                item.setItemMeta(meta);
            }
            titleToItem.put(title.toLowerCase(Locale.ROOT), item);

            ConfigurationSection shooting = s.getConfigurationSection("Shooting");
            String projectile = shooting != null ? shooting.getString("Projectile_Type", "SNOWBALL") : "SNOWBALL";
            double damage = shooting != null ? shooting.getDouble("Projectile_Damage", 4.0) : 4.0;
            double speed = shooting != null ? shooting.getDouble("Projectile_Speed", 1.2) : 1.2;
            double spread = shooting != null ? shooting.getDouble("Bullet_Spread", 2.0) : 2.0;
            // Treat Delay_Between_Shots as ticks per CrackShot; convert to ms
            long cooldown = shooting != null ? (long) Math.max(0, shooting.getInt("Delay_Between_Shots", 5)) * 50L : 250L;
            String singleSound = shooting != null ? firstSoundName(shooting.get("Sounds_Shoot")) : null;
            List<String> soundsShoot = shooting != null ? rawSoundList(shooting.get("Sounds_Shoot")) : Collections.<String>emptyList();
            boolean rightClickToShoot = shooting == null || shooting.getBoolean("Right_Click_To_Shoot", true);
            boolean cancelRightClickInteractions = shooting != null && shooting.getBoolean("Cancel_Right_Click_Interactions", true);
            boolean cancelLeftClickBlockDamage = shooting != null && shooting.getBoolean("Cancel_Left_Click_Block_Damage", true);
            double recoilAmount = shooting != null ? shooting.getDouble("Recoil_Amount", 0.0) : 0.0;
            boolean resetFallDistance = shooting != null && shooting.getBoolean("Reset_Fall_Distance", false);
            int projectileAmount = shooting != null ? shooting.getInt("Projectile_Amount", 1) : 1;
            boolean projectileFlames = shooting != null && shooting.getBoolean("Projectile_Flames", false);
            ConfigurationSection projInc = shooting != null ? shooting.getConfigurationSection("Projectile_Incendiary") : null;
            boolean projectileIncendiaryEnable = projInc != null && projInc.getBoolean("Enable", false);
            int projectileIncendiaryDurationTicks = projInc != null ? projInc.getInt("Duration", 0) : 0;

            // New Shooting fields
            boolean shootingDisable = shooting != null && shooting.getBoolean("Disable", false);
            boolean dualWield = shooting != null && shooting.getBoolean("Dual_Wield", false);
            boolean removeArrowsOnImpact = shooting != null && shooting.getBoolean("Remove_Arrows_On_Impact", false);
            boolean removeBulletDrop = shooting != null && shooting.getBoolean("Remove_Bullet_Drop", false);
            int removalOrDragDelayTicks = 0;
            boolean removalOrDragIsRemoval = true;
            if (shooting != null && shooting.isSet("Removal_Or_Drag_Delay")) {
                Object node = shooting.get("Removal_Or_Drag_Delay");
                if (node instanceof Number) {
                    removalOrDragDelayTicks = ((Number) node).intValue();
                } else if (node instanceof String) {
                    String val = ((String) node).trim();
                    String[] parts = val.split("-", 2);
                    try { removalOrDragDelayTicks = Integer.parseInt(parts[0].trim()); } catch (Exception ignored) {}
                    if (parts.length > 1) {
                        String b = parts[1].trim();
                        removalOrDragIsRemoval = b.equalsIgnoreCase("true");
                    }
                }
            }
            List<String> soundsProjectile = shooting != null ? rawSoundList(shooting.get("Sounds_Projectile")) : Collections.<String>emptyList();

            // Projectile subtype parsing
            String projectileSubtypeRaw = null;
            org.bukkit.Material projectileSubtypeItemMaterial = null;
            int projectileSubtypeItemData = 0;
            boolean fireballCannotBeDeflected = false;
            int energyRange = 0, energyRadius = 0, energyWalls = 0, energyVictims = Integer.MAX_VALUE;
            if (shooting != null && shooting.isSet("Projectile_Subtype")) {
                Object sub = shooting.get("Projectile_Subtype");
                if (sub instanceof Boolean) {
                    fireballCannotBeDeflected = (Boolean) sub;
                    projectileSubtypeRaw = String.valueOf(sub);
                } else if (sub != null) {
                    projectileSubtypeRaw = String.valueOf(sub).trim();
                    if ("ENERGY".equalsIgnoreCase(projectile)) {
                        // RANGE-RADIUS-WALLS-VICTIMS
                        String[] p = projectileSubtypeRaw.split("-");
                        if (p.length >= 2) {
                            try { energyRange = Integer.parseInt(p[0].trim()); } catch (Exception ignored) {}
                            try { energyRadius = Integer.parseInt(p[1].trim()); } catch (Exception ignored) {}
                            if (p.length >= 3) {
                                String wallsStr = p[2].trim().toUpperCase(java.util.Locale.ROOT);
                                if ("NONE".equals(wallsStr)) energyWalls = 0; else if ("ALL".equals(wallsStr)) energyWalls = Integer.MAX_VALUE; else {
                                    try { energyWalls = Integer.parseInt(wallsStr); } catch (Exception ignored) {}
                                }
                            }
                            if (p.length >= 4) {
                                String victimsStr = p[3].trim().toUpperCase(java.util.Locale.ROOT);
                                if ("0".equals(victimsStr)) energyVictims = Integer.MAX_VALUE; else {
                                    try { energyVictims = Integer.parseInt(victimsStr); } catch (Exception ignored) {}
                                }
                            }
                        }
                    } else {
                        // Expect item id or item~data
                        String matStr = projectileSubtypeRaw;
                        int data = 0;
                        if (matStr.contains("~")) {
                            String[] md = matStr.split("~", 2);
                            matStr = md[0].trim();
                            try { data = Integer.parseInt(md[1].trim()); } catch (Exception ignored) {}
                        }
                        org.bukkit.Material m = parseFlexibleMaterial(matStr);
                        if (m != null) {
                            projectileSubtypeItemMaterial = m;
                            projectileSubtypeItemData = data;
                        }
                    }
                }
            }

            ConfigurationSection auto = s.getConfigurationSection("Fully_Automatic");
            boolean fullyAutomatic = auto != null && auto.getBoolean("Enable", false);
            int fireRate = auto != null ? auto.getInt("Fire_Rate", 0) : 0;

            // Burstfire
            ConfigurationSection burst = s.getConfigurationSection("Burstfire");
            boolean burstEnable = burst != null && burst.getBoolean("Enable", false);
            int shotsPerBurst = burst != null ? burst.getInt("Shots_Per_Burst", 1) : 1;
            int delayBetweenShotsInBurstTicks = burst != null ? burst.getInt("Delay_Between_Shots_In_Burst", 0) : 0;

            Map<String, Object> raw = s.getValues(true);

            // Ammo module
            ConfigurationSection ammo = s.getConfigurationSection("Ammo");
            boolean ammoEnable = ammo != null && ammo.getBoolean("Enable", false);
            String ammoItemId = ammo != null ? ammo.getString("Ammo_Item_ID", null) : null;
            // Support both boolean and integer for Take_Ammo_Per_Shot: true->1, false->0, or explicit number
            int ammoTakePerShot = 1;
            if (ammo != null) {
                if (ammo.isBoolean("Take_Ammo_Per_Shot")) {
                    ammoTakePerShot = ammo.getBoolean("Take_Ammo_Per_Shot") ? 1 : 0;
                } else {
                    ammoTakePerShot = Math.max(0, ammo.getInt("Take_Ammo_Per_Shot", 1));
                }
            }
            String ammoNameCheckRaw = ammo != null ? ammo.getString("Ammo_Name_Check", null) : null;
            String ammoNameCheck = ammoNameCheckRaw != null ? ChatColor.translateAlternateColorCodes('&', ammoNameCheckRaw) : null;
            // Separate two sound lists per CrackShot docs
            List<String> ammoSoundsOutOfAmmo = ammo != null ? rawSoundList(ammo.get("Sounds_Out_Of_Ammo")) : Collections.<String>emptyList();
            List<String> ammoSoundsShootWithNoAmmo = ammo != null ? rawSoundList(ammo.get("Sounds_Shoot_With_No_Ammo")) : Collections.<String>emptyList();
            // Parse Ammo_Item_ID supporting ~data and numeric IDs
            org.bukkit.Material ammoMat = null;
            int ammoData = -1;
            if (ammoItemId != null) {
                String idStr = ammoItemId.trim();
                if (idStr.contains("~")) {
                    String[] parts = idStr.split("~", 2);
                    idStr = parts[0].trim();
                    try { ammoData = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) { ammoData = -1; }
                }
                ammoMat = parseFlexibleMaterial(idStr);
            }

            // Reload module
            ConfigurationSection reload = s.getConfigurationSection("Reload");
            boolean reloadEnable = reload != null && reload.getBoolean("Enable", false);
            int reloadAmount = reload != null ? reload.getInt("Reload_Amount", 0) : 0;
            int reloadStartingAmount = reload != null ? reload.getInt("Starting_Amount", reloadAmount) : 0;
            long reloadDurationTicks = reload != null ? (long) reload.getDouble("Reload_Duration", 0.0) : 0L;
            boolean takeAmmoOnReload = reload != null && reload.getBoolean("Take_Ammo_On_Reload", false);
            boolean takeAmmoAsMagazine = reload != null && reload.getBoolean("Take_Ammo_As_Magazine", false);
            List<String> soundsReloading = reload != null ? rawSoundList(reload.get("Sounds_Reloading")) : Collections.<String>emptyList();
            boolean reloadWithMouse = reload != null && reload.getBoolean("Reload_With_Mouse", false);
            boolean reloadBulletsIndividually = reload != null && reload.getBoolean("Reload_Bullets_Individually", false);
            long reloadShootDelayTicks = reload != null ? (long) reload.getDouble("Reload_Shoot_Delay", 0.0) : 0L;
            boolean destroyWhenEmpty = reload != null && reload.getBoolean("Destroy_When_Empty", false);
            List<String> reloadSoundsOutOfAmmo = reload != null ? rawSoundList(reload.get("Sounds_Out_Of_Ammo")) : Collections.<String>emptyList();
            // Dual wield block
            ConfigurationSection dual = reload != null ? reload.getConfigurationSection("Dual_Wield") : null;
            int dualSingleReloadDurationTicks = dual != null ? dual.getInt("Single_Reload_Duration", 0) : 0;
            List<String> dualSoundsSingleReload = dual != null ? rawSoundList(dual.get("Sounds_Single_Reload")) : Collections.<String>emptyList();
            List<String> dualSoundsShootWithNoAmmo = dual != null ? rawSoundList(dual.get("Sounds_Shoot_With_No_Ammo")) : Collections.<String>emptyList();

            // Scope module
            ConfigurationSection scope = s.getConfigurationSection("Scope");
            boolean scopeEnable = scope != null && scope.getBoolean("Enable", false);
            double scopeZoomAmount = scope != null ? scope.getDouble("Zoom_Amount", 0.0) : 0.0;
            double scopeZoomBulletSpread = scope != null ? scope.getDouble("Zoom_Bullet_Spread", spread) : spread;

            // Sneak
            ConfigurationSection sneak = s.getConfigurationSection("Sneak");
            boolean sneakEnable = sneak != null && sneak.getBoolean("Enable", false);
            double sneakBulletSpread = sneak != null ? sneak.getDouble("Bullet_Spread", spread) : spread;

            // Headshot module
            ConfigurationSection headshot = s.getConfigurationSection("Headshot");
            boolean headshotEnable = headshot != null && headshot.getBoolean("Enable", false);
            double headshotBonusDamage = headshot != null ? headshot.getDouble("Bonus_Damage", 0.0) : 0.0;

            // Explosions module
            ConfigurationSection explosions = s.getConfigurationSection("Explosions");
            boolean explosionsEnable = explosions != null && explosions.getBoolean("Enable", false);
            double explosionRadius = explosions != null ? explosions.getDouble("Explosion_Radius", 0.0) : 0.0;
            boolean explosionIncendiary = explosions != null && explosions.getBoolean("Explosion_Incendiary", false);
            boolean explosionNoGrief = explosions != null && explosions.getBoolean("Explosion_No_Grief", true);

            // Firearm_Action
            ConfigurationSection firearm = s.getConfigurationSection("Firearm_Action");
            List<String> firearmSoundClose = firearm != null ? rawSoundList(anyOf(firearm, "Sound_Close")) : Collections.<String>emptyList();
            List<String> firearmSoundOpen = firearm != null ? rawSoundList(anyOf(firearm, "Sound_Open")) : Collections.<String>emptyList();
            String firearmTypeStr = firearm != null ? firearm.getString("Type", null) : null;
            WeaponDefinition.FirearmActionType firearmActionType = WeaponDefinition.FirearmActionType.NONE;
            if (firearmTypeStr != null) {
                String t = firearmTypeStr.trim().toUpperCase(java.util.Locale.ROOT);
                if ("BREAK".equals(t)) firearmActionType = WeaponDefinition.FirearmActionType.BREAK;
                else if ("REVOLVER".equals(t)) firearmActionType = WeaponDefinition.FirearmActionType.REVOLVER;
                else if ("SLIDE".equals(t)) firearmActionType = WeaponDefinition.FirearmActionType.SLIDE;
                else if ("BOLT".equals(t)) firearmActionType = WeaponDefinition.FirearmActionType.BOLT;
                else if ("LEVER".equals(t)) firearmActionType = WeaponDefinition.FirearmActionType.LEVER;
                else if ("PUMP".equals(t)) firearmActionType = WeaponDefinition.FirearmActionType.PUMP;
            }
            int firearmOpenDurationTicks = firearm != null ? firearm.getInt("Open_Duration", 0) : 0;
            int firearmCloseDurationTicks = firearm != null ? firearm.getInt("Close_Duration", 0) : 0;
            int firearmCloseShootDelayTicks = firearm != null ? firearm.getInt("Close_Shoot_Delay", 0) : 0;
            int firearmReloadOpenDelayTicks = firearm != null ? firearm.getInt("Reload_Open_Delay", 0) : 0;
            int firearmReloadCloseDelayTicks = firearm != null ? firearm.getInt("Reload_Close_Delay", 0) : 0;

            // Particles
            ConfigurationSection particles = s.getConfigurationSection("Particles");
            String particlePlayerShoot = particles != null ? firstString(particles.get("Particle_Player_Shoot")) : null;

            // Abilities
            ConfigurationSection abilities = s.getConfigurationSection("Abilities");
            boolean abilityResetHitCooldown = abilities != null && abilities.getBoolean("Reset_Hit_Cooldown", false);
            boolean abilityNoFallDamage = abilities != null && abilities.getBoolean("No_Fall_Damage", false);
            double abilityKnockback = abilities != null ? abilities.getDouble("Knockback", 0.0) : 0.0;

            WeaponDefinition def = new WeaponDefinition(
                    title,
                    material,
                    displayValue,
                    loreValue,
                    projectile,
                    damage,
                    speed,
                    spread,
                    cooldown,
                    singleSound != null ? singleSound : "CHICKEN_EGG_POP",
                    soundsShoot,
                    rightClickToShoot,
                    cancelRightClickInteractions,
                    cancelLeftClickBlockDamage,
                    fullyAutomatic,
                    fireRate,
                    recoilAmount,
                    raw,
                    ammoEnable,
                    ammoItemId,
                    ammoTakePerShot,
                    ammoMat,
                    ammoData,
                    ammoNameCheck,
                    ammoSoundsShootWithNoAmmo,
                    reloadEnable,
                    reloadAmount,
                    reloadStartingAmount,
                    reloadDurationTicks,
                    scopeEnable,
                    scopeZoomAmount,
                    scopeZoomBulletSpread,
                    headshotEnable,
                    headshotBonusDamage,
                    explosionsEnable,
                    explosionRadius,
                    explosionIncendiary,
                    explosionNoGrief,
                    projectileAmount,
                    projectileFlames,
                    projectileIncendiaryEnable,
                    projectileIncendiaryDurationTicks,
                    burstEnable,
                    shotsPerBurst,
                    delayBetweenShotsInBurstTicks,
                    sneakEnable,
                    sneakBulletSpread,
                    takeAmmoOnReload,
                    takeAmmoAsMagazine,
                    ammoSoundsOutOfAmmo,
                    soundsReloading,
                    firearmSoundClose,
                    firearmActionType,
                    firearmOpenDurationTicks,
                    firearmCloseDurationTicks,
                    firearmCloseShootDelayTicks,
                    firearmReloadOpenDelayTicks,
                    firearmReloadCloseDelayTicks,
                    firearmSoundOpen,
                    particlePlayerShoot,
                    abilityResetHitCooldown,
                    abilityNoFallDamage,
                    abilityKnockback,
                    resetFallDistance,
                    reloadWithMouse,
                    reloadBulletsIndividually,
                    reloadShootDelayTicks,
                    destroyWhenEmpty,
                    reloadSoundsOutOfAmmo,
                    dualSingleReloadDurationTicks,
                    dualSoundsSingleReload,
                    dualSoundsShootWithNoAmmo,
                    shootingDisable,
                    dualWield,
                    removeArrowsOnImpact,
                    removeBulletDrop,
                    removalOrDragDelayTicks,
                    removalOrDragIsRemoval,
                    soundsProjectile,
                    projectileSubtypeRaw,
                    projectileSubtypeItemMaterial,
                    projectileSubtypeItemData,
                    fireballCannotBeDeflected,
                    energyRange,
                    energyRadius,
                    energyWalls,
                    energyVictims
            );
            titleToDef.put(title.toLowerCase(Locale.ROOT), def);
        }
    }

    private Material parseFlexibleMaterial(String input) {
        if (input == null) return null;
        String s = input.trim();
        // Handle dye like 351~X -> map to color dyes approximately
        if (s.startsWith("351~")) {
            try {
                int data = Integer.parseInt(s.substring(4));
                // Legacy Bukkit (1.8) does not expose dye colors as separate materials.
                // We fallback to INK_SACK so at least the item type exists.
                return Material.INK_SACK;
            } catch (Exception ignored) {}
        }
        // Numeric IDs not supported in modern Bukkit; fall back to common guesses
        try {
            int id = Integer.parseInt(s);
            switch (id) {
                case 256: return Material.IRON_SPADE;
                case 257: return Material.IRON_PICKAXE;
                case 258: return Material.IRON_AXE;
                case 259: return Material.FLINT_AND_STEEL;
                case 260: return Material.APPLE;
                case 269: return Material.WOOD_SPADE;
                case 270: return Material.WOOD_PICKAXE;
                case 271: return Material.WOOD_AXE;
                case 272: return Material.STONE_SWORD;
                case 273: return Material.STONE_SPADE;
                case 274: return Material.STONE_PICKAXE;
                case 275: return Material.STONE_AXE;
                case 276: return Material.DIAMOND_SWORD;
                case 277: return Material.DIAMOND_SPADE;
                case 278: return Material.DIAMOND_PICKAXE;
                case 279: return Material.DIAMOND_AXE;
                case 281: return Material.BOWL;
                case 284: return Material.GOLD_SPADE;
                case 285: return Material.GOLD_PICKAXE;
                case 286: return Material.GOLD_AXE;
                case 290: return Material.WOOD_HOE;
                case 292: return Material.STONE_HOE;
                case 294: return Material.GOLD_HOE;
                case 337: return Material.CLAY_BALL;
                case 369: return Material.BLAZE_ROD;
                case 410: return Material.HOPPER;
                default: break;
            }
        } catch (NumberFormatException ignored) {}
        return Material.matchMaterial(s);
    }

    private String firstSoundName(Object node) {
        List<String> list = normalizeSoundList(node);
        return list.isEmpty() ? null : list.get(0);
    }

    private Object anyOf(ConfigurationSection sec, String... keys) {
        if (sec == null) return null;
        for (String k : keys) {
            if (sec.isList(k) || sec.isString(k)) return sec.get(k);
        }
        return null;
    }

    private List<String> normalizeSoundList(Object node) {
        if (node == null) return Collections.emptyList();
        List<String> out = new ArrayList<String>();
        if (node instanceof List) {
            for (Object o : (List<?>) node) {
                if (o == null) continue;
                String s = String.valueOf(o);
                String name = s.split("-", 2)[0];
                out.add(name);
            }
        } else if (node instanceof String) {
            String s = (String) node;
            for (String part : s.split(",")) {
                String name = part.trim().split("-", 2)[0];
                if (!name.isEmpty()) out.add(name);
            }
        }
        return out;
    }

    private String firstString(Object node) {
        if (node == null) return null;
        if (node instanceof String) return (String) node;
        if (node instanceof List) {
            List<?> l = (List<?>) node;
            return l.isEmpty() ? null : String.valueOf(l.get(0));
        }
        return String.valueOf(node);
    }

    // Return raw list preserving CrackShot format NAME-vol-pitch-delay
    private List<String> rawSoundList(Object node) {
        if (node == null) return Collections.emptyList();
        List<String> out = new ArrayList<String>();
        if (node instanceof List) {
            for (Object o : (List<?>) node) {
                if (o == null) continue;
                out.add(String.valueOf(o).trim());
            }
        } else if (node instanceof String) {
            String s = (String) node;
            for (String part : s.split(",")) {
                String one = part.trim();
                if (!one.isEmpty()) out.add(one);
            }
        }
        return out;
    }

    public ItemStack generateWeapon(String weaponTitle) {
        ItemStack base = titleToItem.get(weaponTitle.toLowerCase(Locale.ROOT));
        if (base == null) return null;
        return base.clone();
    }

    public String getWeaponTitle(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return null;
        String display = ChatColor.stripColor(meta.getDisplayName()).toLowerCase(Locale.ROOT);
        // Strip dynamic ammo suffix placed at the end, supporting [], <>, (), {}
        display = stripTrailingBracketSuffix(display);
        for (Map.Entry<String, ItemStack> e : titleToItem.entrySet()) {
            ItemStack v = e.getValue();
            ItemMeta m = v.getItemMeta();
            if (m != null && m.hasDisplayName()) {
                String vd = ChatColor.stripColor(m.getDisplayName()).toLowerCase(Locale.ROOT);
                if (vd.equals(display)) return e.getKey();
                // Also compare after stripping suffix from stored item names, to be safe
                if (stripTrailingBracketSuffix(vd).equals(display)) return e.getKey();
            }
        }
        return null;
    }

    private String stripTrailingBracketSuffix(String s) {
        if (s == null) return null;
        String out = s;
        // Strip trailing superscript r (modifier letter small r) if present
        while (out.endsWith("\u02B3")) {
            out = out.substring(0, out.length() - 1);
        }
        char[][] pairs = new char[][] { {'[', ']'}, {'<', '>'}, {'(', ')'}, {'{', '}'}, {'\u00AB', '\u00BB'} };
        for (char[] p : pairs) {
            int idx = out.lastIndexOf(p[0]);
            boolean endsClose = out.endsWith(String.valueOf(p[1]));
            boolean endsCloseR = out.endsWith(String.valueOf(p[1]) + "\u02B3");
            if (idx > 0 && (endsClose || endsCloseR)) {
                String base = out.substring(0, idx).trim();
                if (!base.isEmpty()) out = base;
            }
        }
        return out;
    }

    public WeaponDefinition getDefinition(String title) {
        if (title == null) return null;
        return titleToDef.get(title.toLowerCase(Locale.ROOT));
    }
}


