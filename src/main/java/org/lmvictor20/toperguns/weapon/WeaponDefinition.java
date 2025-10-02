package org.lmvictor20.toperguns.weapon;

import org.bukkit.Material;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WeaponDefinition {
    public final String title;
    public final Material material;
    public final String display;
    public final java.util.List<String> lore;

    public final String projectile;
    public final double damage;
    public final double speed;
    public final double spread;
    public final long cooldownMs;

    // Legacy single sound support (backwards compatibility)
    public final String sound;
    // CrackShot-like fields
    public final List<String> soundsShoot;
    public final boolean rightClickToShoot;
    public final boolean cancelRightClickInteractions;
    public final boolean cancelLeftClickBlockDamage;
    public final boolean fullyAutomatic;
    public final int fireRate; // shots per second when fully automatic
    public final double recoilAmount; // basic recoil magnitude
    // Removed flag: recoil now always overrides velocity for consistent jump
    public final boolean recoilOverrideVelocity = true;

    // Store raw modules for future use/expansion
    public final Map<String, Object> rawModules;

    // Ammo module
    public final boolean ammoEnable;
    public final String ammoItemId; // Bukkit Material name or legacy id string
    public final int ammoTakePerShot;
    public final org.bukkit.Material ammoItemMaterial; // parsed from Ammo_Item_ID
    public final int ammoItemData; // -1 means any data
    public final String ammoNameCheck; // translated &-codes allowed; substring match
    public final List<String> ammoSoundsShootWithNoAmmo; // when attempting to shoot without any ammo in inventory

    // Reload module
    public final boolean reloadEnable;
    public final int reloadAmount; // magazine size
    public final int reloadStartingAmount; // starting bullets in mag
    public final long reloadDurationTicks; // duration in ticks

    // Scope module
    public final boolean scopeEnable;
    public final double scopeZoomAmount;
    public final double scopeZoomBulletSpread;

    // Headshot module
    public final boolean headshotEnable;
    public final double headshotBonusDamage;

    // Explosions module
    public final boolean explosionsEnable;
    public final double explosionRadius;
    public final boolean explosionIncendiary;
    public final boolean explosionNoGrief;

    // Additional CrackShot fields
    public final int projectileAmount;
    public final boolean projectileFlames;
    public final boolean projectileIncendiaryEnable;
    public final int projectileIncendiaryDurationTicks;
    public final boolean burstEnable;
    public final int shotsPerBurst;
    public final int delayBetweenShotsInBurstTicks;
    public final boolean sneakEnable;
    public final double sneakBulletSpread;
    // Reload flags and sounds
    public final boolean takeAmmoOnReload;
    public final boolean takeAmmoAsMagazine;
    public final List<String> soundsOutOfAmmo;
    public final List<String> soundsReloading;
    // Firearm action sounds
    public final List<String> firearmSoundClose;
    // Firearm action module
    public enum FirearmActionType { NONE, SLIDE, BOLT, LEVER, PUMP, BREAK, REVOLVER }
    public final FirearmActionType firearmActionType;
    public final int firearmOpenDurationTicks;
    public final int firearmCloseDurationTicks;
    public final int firearmCloseShootDelayTicks;
    public final int firearmReloadOpenDelayTicks;
    public final int firearmReloadCloseDelayTicks;
    public final List<String> firearmSoundOpen;
    // Particles
    public final String particlePlayerShoot;
    // Abilities subset
    public final boolean abilityResetHitCooldown;
    public final boolean abilityNoFallDamage;
    public final double abilityKnockback;
    // Shooting flags
    public final boolean resetFallDistance;

    // Reload extras (CrackShot compatibility)
    public final boolean reloadWithMouse;
    public final boolean reloadBulletsIndividually;
    public final long reloadShootDelayTicks;
    public final boolean destroyWhenEmpty;
    public final List<String> reloadSoundsOutOfAmmo;
    // Dual wield (parsed only)
    public final int dualSingleReloadDurationTicks;
    public final List<String> dualSoundsSingleReload;
    public final List<String> dualSoundsShootWithNoAmmo;

    // New Shooting fields (CrackShot compatibility)
    public final boolean shootingDisable; // Shooting.Disable
    public final boolean dualWield; // Shooting.Dual_Wield
    public final boolean removeArrowsOnImpact; // Shooting.Remove_Arrows_On_Impact
    public final boolean removeBulletDrop; // Shooting.Remove_Bullet_Drop
    public final int removalOrDragDelayTicks; // Shooting.Removal_Or_Drag_Delay: delay
    public final boolean removalOrDragIsRemoval; // Shooting.Removal_Or_Drag_Delay: true->remove, false->drag
    public final List<String> soundsProjectile; // Shooting.Sounds_Projectile
    public final String projectileSubtypeRaw; // raw string for reference
    public final org.bukkit.Material projectileSubtypeItemMaterial; // for grenade/flare/splash item id
    public final int projectileSubtypeItemData; // for grenade/flare/splash item data
    public final boolean fireballCannotBeDeflected; // fireball subtype true
    public final int energyRange; // blocks forward (ENERGY)
    public final int energyRadius; // half-size of cuboid (ENERGY)
    public final int energyWalls; // walls penetration count (ENERGY). Integer.MAX_VALUE for ALL, 0 for NONE
    public final int energyVictims; // victims penetration count (ENERGY). Integer.MAX_VALUE for unlimited

    public WeaponDefinition(
            String title,
            Material material,
            String display,
            List<String> lore,
            String projectile,
            double damage,
            double speed,
            double spread,
            long cooldownMs,
            String sound,
            List<String> soundsShoot,
            boolean rightClickToShoot,
            boolean cancelRightClickInteractions,
            boolean cancelLeftClickBlockDamage,
            boolean fullyAutomatic,
            int fireRate,
            double recoilAmount,
            Map<String, Object> rawModules,
            boolean ammoEnable,
            String ammoItemId,
            int ammoTakePerShot,
            org.bukkit.Material ammoItemMaterial,
            int ammoItemData,
            String ammoNameCheck,
            List<String> ammoSoundsShootWithNoAmmo,
            boolean reloadEnable,
            int reloadAmount,
            int reloadStartingAmount,
            long reloadDurationTicks,
            boolean scopeEnable,
            double scopeZoomAmount,
            double scopeZoomBulletSpread,
            boolean headshotEnable,
            double headshotBonusDamage,
            boolean explosionsEnable,
            double explosionRadius,
            boolean explosionIncendiary,
            boolean explosionNoGrief,
            int projectileAmount,
            boolean projectileFlames,
            boolean projectileIncendiaryEnable,
            int projectileIncendiaryDurationTicks,
            boolean burstEnable,
            int shotsPerBurst,
            int delayBetweenShotsInBurstTicks,
            boolean sneakEnable,
            double sneakBulletSpread,
            boolean takeAmmoOnReload,
            boolean takeAmmoAsMagazine,
            List<String> soundsOutOfAmmo,
            List<String> soundsReloading,
            List<String> firearmSoundClose,
            FirearmActionType firearmActionType,
            int firearmOpenDurationTicks,
            int firearmCloseDurationTicks,
            int firearmCloseShootDelayTicks,
            int firearmReloadOpenDelayTicks,
            int firearmReloadCloseDelayTicks,
            List<String> firearmSoundOpen,
            String particlePlayerShoot,
            boolean abilityResetHitCooldown,
            boolean abilityNoFallDamage,
            double abilityKnockback,
            boolean resetFallDistance,
            boolean reloadWithMouse,
            boolean reloadBulletsIndividually,
            long reloadShootDelayTicks,
            boolean destroyWhenEmpty,
            List<String> reloadSoundsOutOfAmmo,
            int dualSingleReloadDurationTicks,
            List<String> dualSoundsSingleReload,
            List<String> dualSoundsShootWithNoAmmo,
            boolean shootingDisable,
            boolean dualWield,
            boolean removeArrowsOnImpact,
            boolean removeBulletDrop,
            int removalOrDragDelayTicks,
            boolean removalOrDragIsRemoval,
            List<String> soundsProjectile,
            String projectileSubtypeRaw,
            org.bukkit.Material projectileSubtypeItemMaterial,
            int projectileSubtypeItemData,
            boolean fireballCannotBeDeflected,
            int energyRange,
            int energyRadius,
            int energyWalls,
            int energyVictims
    ) {
        this.title = title;
        this.material = material;
        this.display = display;
        this.lore = lore;
        this.projectile = projectile;
        this.damage = damage;
        this.speed = speed;
        this.spread = spread;
        this.cooldownMs = cooldownMs;
        this.sound = sound;
        this.soundsShoot = soundsShoot == null ? Collections.emptyList() : soundsShoot;
        this.rightClickToShoot = rightClickToShoot;
        this.cancelRightClickInteractions = cancelRightClickInteractions;
        this.cancelLeftClickBlockDamage = cancelLeftClickBlockDamage;
        this.fullyAutomatic = fullyAutomatic;
        this.fireRate = fireRate;
        this.recoilAmount = recoilAmount;
        this.rawModules = rawModules == null ? Collections.emptyMap() : rawModules;

        this.ammoEnable = ammoEnable;
        this.ammoItemId = ammoItemId;
        this.ammoTakePerShot = ammoTakePerShot;
        this.ammoItemMaterial = ammoItemMaterial;
        this.ammoItemData = ammoItemData;
        this.ammoNameCheck = ammoNameCheck;
        this.ammoSoundsShootWithNoAmmo = ammoSoundsShootWithNoAmmo == null ? Collections.<String>emptyList() : ammoSoundsShootWithNoAmmo;
        this.reloadEnable = reloadEnable;
        this.reloadAmount = reloadAmount;
        this.reloadStartingAmount = reloadStartingAmount;
        this.reloadDurationTicks = reloadDurationTicks;
        this.scopeEnable = scopeEnable;
        this.scopeZoomAmount = scopeZoomAmount;
        this.scopeZoomBulletSpread = scopeZoomBulletSpread;
        this.headshotEnable = headshotEnable;
        this.headshotBonusDamage = headshotBonusDamage;
        this.explosionsEnable = explosionsEnable;
        this.explosionRadius = explosionRadius;
        this.explosionIncendiary = explosionIncendiary;
        this.explosionNoGrief = explosionNoGrief;

        this.projectileAmount = projectileAmount;
        this.projectileFlames = projectileFlames;
        this.projectileIncendiaryEnable = projectileIncendiaryEnable;
        this.projectileIncendiaryDurationTicks = projectileIncendiaryDurationTicks;
        this.burstEnable = burstEnable;
        this.shotsPerBurst = shotsPerBurst;
        this.delayBetweenShotsInBurstTicks = delayBetweenShotsInBurstTicks;
        this.sneakEnable = sneakEnable;
        this.sneakBulletSpread = sneakBulletSpread;
        this.takeAmmoOnReload = takeAmmoOnReload;
        this.takeAmmoAsMagazine = takeAmmoAsMagazine;
        this.soundsOutOfAmmo = soundsOutOfAmmo == null ? Collections.<String>emptyList() : soundsOutOfAmmo;
        this.soundsReloading = soundsReloading == null ? Collections.<String>emptyList() : soundsReloading;
        this.firearmSoundClose = firearmSoundClose == null ? Collections.<String>emptyList() : firearmSoundClose;
        this.firearmActionType = firearmActionType == null ? FirearmActionType.NONE : firearmActionType;
        this.firearmOpenDurationTicks = firearmOpenDurationTicks;
        this.firearmCloseDurationTicks = firearmCloseDurationTicks;
        this.firearmCloseShootDelayTicks = firearmCloseShootDelayTicks;
        this.firearmReloadOpenDelayTicks = firearmReloadOpenDelayTicks;
        this.firearmReloadCloseDelayTicks = firearmReloadCloseDelayTicks;
        this.firearmSoundOpen = firearmSoundOpen == null ? Collections.<String>emptyList() : firearmSoundOpen;
        this.particlePlayerShoot = particlePlayerShoot;
        this.abilityResetHitCooldown = abilityResetHitCooldown;
        this.abilityNoFallDamage = abilityNoFallDamage;
        this.abilityKnockback = abilityKnockback;
        this.resetFallDistance = resetFallDistance;

        this.reloadWithMouse = reloadWithMouse;
        this.reloadBulletsIndividually = reloadBulletsIndividually;
        this.reloadShootDelayTicks = reloadShootDelayTicks;
        this.destroyWhenEmpty = destroyWhenEmpty;
        this.reloadSoundsOutOfAmmo = reloadSoundsOutOfAmmo == null ? Collections.<String>emptyList() : reloadSoundsOutOfAmmo;
        this.dualSingleReloadDurationTicks = dualSingleReloadDurationTicks;
        this.dualSoundsSingleReload = dualSoundsSingleReload == null ? Collections.<String>emptyList() : dualSoundsSingleReload;
        this.dualSoundsShootWithNoAmmo = dualSoundsShootWithNoAmmo == null ? Collections.<String>emptyList() : dualSoundsShootWithNoAmmo;

        this.shootingDisable = shootingDisable;
        this.dualWield = dualWield;
        this.removeArrowsOnImpact = removeArrowsOnImpact;
        this.removeBulletDrop = removeBulletDrop;
        this.removalOrDragDelayTicks = Math.max(0, removalOrDragDelayTicks);
        this.removalOrDragIsRemoval = removalOrDragIsRemoval;
        this.soundsProjectile = soundsProjectile == null ? Collections.<String>emptyList() : soundsProjectile;
        this.projectileSubtypeRaw = projectileSubtypeRaw;
        this.projectileSubtypeItemMaterial = projectileSubtypeItemMaterial;
        this.projectileSubtypeItemData = projectileSubtypeItemData;
        this.fireballCannotBeDeflected = fireballCannotBeDeflected;
        this.energyRange = Math.max(0, energyRange);
        this.energyRadius = Math.max(0, energyRadius);
        this.energyWalls = energyWalls;
        this.energyVictims = energyVictims;
    }

    // Backwards compatible constructor for legacy simple weapons.yml
    public WeaponDefinition(String title, Material material, String display, List<String> lore,
                            String projectile, double damage, double speed, double spread, long cooldownMs, String sound) {
        this(
                title,
                material,
                display,
                lore,
                projectile,
                damage,
                speed,
                spread,
                cooldownMs,
                sound,
                Collections.<String>emptyList(),
                true,
                true,
                true,
                false,
                0,
                0.0,
                Collections.<String, Object>emptyMap(),
                false,
                "",
                0,
                null,
                -1,
                null,
                Collections.<String>emptyList(),
                false,
                0,
                0,
                0L,
                false,
                0.0,
                0.0,
                false,
                0.0,
                false,
                0.0,
                false,
                true,
                1,
                false,
                false,
                0,
                false,
                1,
                0,
                false,
                0.0,
                false,
                false,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                FirearmActionType.NONE,
                0,
                0,
                0,
                0,
                0,
                Collections.<String>emptyList(),
                null,
                false,
                false,
                0.0,
                false,
                false,
                false,
                0L,
                false,
                Collections.<String>emptyList(),
                0,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                false,
                false,
                false,
                false,
                0,
                true,
                Collections.<String>emptyList(),
                null,
                null,
                0,
                false,
                0,
                0,
                0,
                Integer.MAX_VALUE
        );
    }
}


