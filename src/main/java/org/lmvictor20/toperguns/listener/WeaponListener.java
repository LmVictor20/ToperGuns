package org.lmvictor20.toperguns.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.lmvictor20.toperguns.ToperGunsPlugin;
import org.lmvictor20.toperguns.event.WeaponEvents;
import org.lmvictor20.toperguns.weapon.WeaponDefinition;
import org.lmvictor20.toperguns.weapon.WeaponManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class WeaponListener implements Listener {

    private final ToperGunsPlugin plugin;
    private final Map<String, Long> nextShotAt = new HashMap<String, Long>();
    private final Random random = new Random();
    // Ammo and reload tracking per player+weaponTitle
    private final Map<String, Integer> magazine = new HashMap<String, Integer>();
    private final Map<String, Long> reloadingUntil = new HashMap<String, Long>();
    private final Map<String, String> currentWeaponByPlayer = new HashMap<String, String>();
    // Firearm action open-state tracking for BREAK/REVOLVER
    private final Map<String, Boolean> actionOpen = new HashMap<String, Boolean>();
    // Reload generation tokens to cancel pending scheduled tasks when interrupted
    private final Map<String, Integer> reloadGeneration = new HashMap<String, Integer>();

    public WeaponListener(ToperGunsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK && a != Action.LEFT_CLICK_AIR && a != Action.LEFT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        ItemStack inHand = p.getItemInHand();
        if (inHand == null || inHand.getType() == Material.AIR) return;
        WeaponManager wm = plugin.getWeaponManager();
        String title = wm.getWeaponTitle(inHand);
        if (title == null) { return; }
        WeaponDefinition def = plugin.getWeaponManager().getDefinition(title);
        if (def == null) { return; }
        currentWeaponByPlayer.put(p.getName(), title);

        // Shooting disabled entirely
        if (def.shootingDisable) {
            return;
        }
        // Respect CrackShot-like click bindings; Dual_Wield forces both sides allowed
        boolean isRightClick = (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK);
        boolean isLeftClick = (a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK);
        if (!def.dualWield) {
            if (def.rightClickToShoot && !isRightClick) {
                return;
            }
            if (!def.rightClickToShoot && !isLeftClick) {
                return;
            }
        }

        // Cancel interactions based on config
        if ((isRightClick && def.cancelRightClickInteractions) || (isLeftClick && def.cancelLeftClickBlockDamage)) {
            e.setCancelled(true);
        }

        WeaponEvents.WeaponPrepareShootEvent prepare = new WeaponEvents.WeaponPrepareShootEvent(p, title);
        Bukkit.getPluginManager().callEvent(prepare);
        if (prepare.isCancelled()) { e.setCancelled(true); return; }

        double effectiveSpread = def.spread;
        // Scope & Sneak spread
        if (def.scopeEnable && p.isSneaking() && def.scopeZoomBulletSpread > 0.0) {
            effectiveSpread = Math.max(0.0, def.scopeZoomBulletSpread);
        } else if (def.sneakEnable && p.isSneaking()) {
            effectiveSpread = Math.max(0.0, def.sneakBulletSpread);
        }

        long now = System.currentTimeMillis();
        long next = nextShotAt.containsKey(p.getName()) ? nextShotAt.get(p.getName()) : 0L;
        if (now < next) { return; }

        // Handle reload state
        String key = p.getName().toLowerCase(Locale.ROOT) + "|" + title.toLowerCase(Locale.ROOT);
        if (reloadingUntil.containsKey(key)) {
            long until = reloadingUntil.get(key);
            if (now < until) {
                // If BREAK/REVOLVER and not dual-wield, allow forced close interrupt
                if (!def.dualWield && (def.firearmActionType == org.lmvictor20.toperguns.weapon.WeaponDefinition.FirearmActionType.BREAK
                        || def.firearmActionType == org.lmvictor20.toperguns.weapon.WeaponDefinition.FirearmActionType.REVOLVER)) {
                    e.setCancelled(true);
                    // cancel current reload by advancing generation and clearing lock
                    int gen = (reloadGeneration.containsKey(key) ? reloadGeneration.get(key) : 0) + 1;
                    reloadGeneration.put(key, gen);
                    reloadingUntil.remove(key);
                    // Perform close action now
                    int closeTicks = Math.max(0, def.firearmCloseDurationTicks);
                    int shootDelayTicks = Math.max(0, def.firearmCloseShootDelayTicks);
                    if (def.firearmSoundClose != null && !def.firearmSoundClose.isEmpty()) {
                        playSounds(p, def.firearmSoundClose);
                    }
                    if (closeTicks > 0) {
                        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                            @Override
                            public void run() { actionOpen.put(key, false); }
                        }, closeTicks);
                    } else {
                        actionOpen.put(key, false);
                    }
                    long holdMs = (long) (closeTicks + shootDelayTicks) * 50L;
                    if (holdMs > 0) {
                        nextShotAt.put(p.getName(), System.currentTimeMillis() + holdMs);
                    }
                    return;
                }
                return; // still reloading for other types
            } else {
                reloadingUntil.remove(key);
            }
        }

        // If BREAK/REVOLVER is open, block usage and close first
        if (!def.dualWield && (def.firearmActionType == org.lmvictor20.toperguns.weapon.WeaponDefinition.FirearmActionType.BREAK
                || def.firearmActionType == org.lmvictor20.toperguns.weapon.WeaponDefinition.FirearmActionType.REVOLVER)) {
            if (Boolean.TRUE.equals(actionOpen.get(key))) {
                e.setCancelled(true);
                // Start closing immediately using configured delays
                int closeTicks = Math.max(0, def.firearmCloseDurationTicks);
                int shootDelayTicks = Math.max(0, def.firearmCloseShootDelayTicks);
                if (def.firearmSoundClose != null && !def.firearmSoundClose.isEmpty()) {
                    playSounds(p, def.firearmSoundClose);
                }
                // Mark will be closed after close duration
                if (closeTicks > 0) {
                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() { actionOpen.put(key, false); }
                    }, closeTicks);
                } else {
                    actionOpen.put(key, false);
                }
                long holdMs = (long) (closeTicks + shootDelayTicks) * 50L;
                if (holdMs > 0) {
                    nextShotAt.put(p.getName(), System.currentTimeMillis() + holdMs);
                }
                return;
            }
        }

        // Initialize magazine if needed
        if (def.reloadEnable && def.reloadAmount > 0) {
            if (!magazine.containsKey(key)) {
                int start = inferCurrentAmmoFromItem(inHand, def);
                magazine.put(key, start);
                updateHeldItemDisplay(p, def, start);
            }
            int currentMag = Math.max(0, magazine.get(key));
            // Mouse-based reload: trigger reload on click when not full
            if (def.reloadWithMouse && currentMag < def.reloadAmount) {
                beginReload(p, def, key, now);
                return;
            }
            if (currentMag <= 0) {
                // If reload consumes ammo and ammo id present, check first
                if (def.ammoEnable && (def.takeAmmoOnReload || def.takeAmmoAsMagazine)
                        && def.ammoItemId != null && !def.ammoItemId.isEmpty()) {
                    if (!canReloadWithAmmo(p.getInventory(), def)) {
                        // Prefer reload module's out-of-ammo sounds if provided
                        if (def.reloadSoundsOutOfAmmo != null && !def.reloadSoundsOutOfAmmo.isEmpty()) {
                            playSounds(p, def.reloadSoundsOutOfAmmo);
                        } else {
                            playSounds(p, def.soundsOutOfAmmo);
                        }
                        return;
                    }
                }
                // attempt reload
                beginReload(p, def, key, now);
                return;
            }
        } else {
            // Ammo per shot without reload
            if (def.ammoEnable && (def.ammoItemId != null && !def.ammoItemId.isEmpty())) {
                int per = Math.max(1, def.ammoTakePerShot);
                int before = countAmmoItems(p.getInventory(), def);
                if (before <= 0) {
                    playSounds(p, def.ammoSoundsShootWithNoAmmo);
                    return;
                }
                if (!consumeAmmoItems(p.getInventory(), def, per)) {
                    playSounds(p, def.ammoSoundsShootWithNoAmmo);
                    return;
                }
                int after = countAmmoItems(p.getInventory(), def);
                if (after <= 0) {
                    playSounds(p, def.soundsOutOfAmmo);
                }
            }
            // If no reload system, decrement a synthetic counter for display only
            String keyNoReload = p.getName().toLowerCase(Locale.ROOT) + "|" + title.toLowerCase(Locale.ROOT) + "|noreload";
            int remaining = magazine.containsKey(keyNoReload) ? magazine.get(keyNoReload) : def.reloadAmount > 0 ? def.reloadAmount : 1;
            remaining = Math.max(0, remaining - 1);
            magazine.put(keyNoReload, remaining);
            updateHeldItemDisplay(p, def, remaining);
        }

        WeaponEvents.WeaponPreShootEvent pre = new WeaponEvents.WeaponPreShootEvent(p, title, effectiveSpread);
        Bukkit.getPluginManager().callEvent(pre);
        if (pre.isCancelled()) { e.setCancelled(true); return; }

        // Apply abilities: no fall damage right after shooting
        if (def.resetFallDistance) {
            p.setFallDistance(0f);
        }

        // Spawn projectile(s) with speed and spread, extended types
        int count = Math.max(0, def.projectileAmount);
        if ("ENERGY".equalsIgnoreCase(def.projectile)) {
            // Hitscan-like cuboid in front of player
            handleEnergyShot(p, def);
        } else if ("SPLASH".equalsIgnoreCase(def.projectile)) {
            for (int i = 0; i < count; i++) {
                org.bukkit.entity.ThrownPotion potion = p.launchProjectile(org.bukkit.entity.ThrownPotion.class);
                applyCommonProjectileProps(potion, p, def, pre);
            }
        } else if ("WITHERSKULL".equalsIgnoreCase(def.projectile)) {
            for (int i = 0; i < count; i++) {
                org.bukkit.entity.WitherSkull skull = p.launchProjectile(org.bukkit.entity.WitherSkull.class);
                applyCommonProjectileProps(skull, p, def, pre);
            }
        } else if ("GRENADE".equalsIgnoreCase(def.projectile) || "FLARE".equalsIgnoreCase(def.projectile)) {
            // Thrown item that explodes (grenade) or triggers airstrike later (flare)
            for (int i = 0; i < count; i++) {
                ItemStack stack = new ItemStack(def.projectileSubtypeItemMaterial != null ? def.projectileSubtypeItemMaterial : Material.SLIME_BALL, 1, (short) def.projectileSubtypeItemData);
                org.bukkit.entity.Item drop = p.getWorld().dropItem(p.getEyeLocation(), stack);
                org.bukkit.util.Vector dir = p.getLocation().getDirection().normalize();
                // Apply spread
                double sp = Math.toRadians(Math.max(0.0, pre.getBulletSpread()));
                dir.setX(dir.getX() + (random.nextDouble() - 0.5) * sp);
                dir.setY(dir.getY() + (random.nextDouble() - 0.5) * sp);
                dir.setZ(dir.getZ() + (random.nextDouble() - 0.5) * sp);
                double speedMul = Math.max(0.1, def.speed);
                drop.setVelocity(dir.multiply(speedMul));
                drop.setPickupDelay(32767);
                // Schedule explode/airstrike
                scheduleGrenadeOrFlare(p, def, drop, "GRENADE".equalsIgnoreCase(def.projectile));
            }
        } else {
            for (int i = 0; i < count; i++) {
                Projectile proj;
                if ("SNOWBALL".equalsIgnoreCase(def.projectile)) {
                    proj = p.launchProjectile(Snowball.class);
                } else if ("EGG".equalsIgnoreCase(def.projectile)) {
                    proj = p.launchProjectile(Egg.class);
                } else if ("ARROW".equalsIgnoreCase(def.projectile)) {
                    proj = p.launchProjectile(Arrow.class);
                } else if ("FIREBALL".equalsIgnoreCase(def.projectile)) {
                    proj = p.launchProjectile(Fireball.class);
                } else {
                    proj = p.launchProjectile(Snowball.class);
                }
                applyCommonProjectileProps(proj, p, def, pre);
            }
        }

        // Recoil: always override current velocity for consistent flight
        if (def.recoilAmount != 0.0) {
            org.bukkit.util.Vector dir = p.getLocation().getDirection().normalize();
            org.bukkit.util.Vector recoil = dir.multiply(-def.recoilAmount / 10.0);
            p.setVelocity(recoil);
        }

        // Consume one bullet from magazine if enabled
        if (def.reloadEnable && def.reloadAmount > 0) {
            int currentMag = Math.max(0, magazine.get(key));
            if (currentMag > 0) {
                magazine.put(key, currentMag - 1);
                if (currentMag - 1 <= 0) {
                    beginReload(p, def, key, now);
                }
                // Update display name with dynamic ammo count
                updateHeldItemDisplay(p, def, currentMag - 1);
            }
        }

        // Sound: prefer Sounds_Shoot list, fallback to legacy sound; also play projectile sounds
        if (def.soundsShoot != null && !def.soundsShoot.isEmpty()) {
            playSounds(p, def.soundsShoot);
        } else if (def.sound != null && !def.sound.isEmpty()) {
            Sound s = resolveSoundCompat(def.sound.toUpperCase(Locale.ROOT));
            if (s != null) {
                Location loc = p.getLocation();
                p.getWorld().playSound(loc, s, 1.0f, 1.0f);
            }
        }
        if (def.soundsProjectile != null && !def.soundsProjectile.isEmpty()) {
            playSounds(p, def.soundsProjectile);
        }

        // Particles on player shoot (basic mapping to SMOKE)
        if (def.particlePlayerShoot != null) {
            try {
                p.getWorld().playEffect(p.getLocation(), org.bukkit.Effect.SMOKE, 4);
            } catch (Exception ignored) {}
        }

        // Fire-rate/cooldown management
        long delayMs = Math.max(0L, def.cooldownMs);
        if (def.fullyAutomatic && def.fireRate > 0) {
            delayMs = Math.max(10L, 1000L / Math.max(1, def.fireRate));
        }
        // Firearm_Action overrides rate of fire for certain types
        if (!def.dualWield && (def.firearmActionType == org.lmvictor20.toperguns.weapon.WeaponDefinition.FirearmActionType.BOLT
                || def.firearmActionType == org.lmvictor20.toperguns.weapon.WeaponDefinition.FirearmActionType.LEVER
                || def.firearmActionType == org.lmvictor20.toperguns.weapon.WeaponDefinition.FirearmActionType.PUMP)) {
            long actionTicks = Math.max(0, def.firearmOpenDurationTicks) + Math.max(0, def.firearmCloseDurationTicks) + Math.max(0, def.firearmCloseShootDelayTicks);
            if (actionTicks > 0) {
                delayMs = actionTicks * 50L;
            }
            // Play open immediately, close after open duration
            if (def.firearmSoundOpen != null && !def.firearmSoundOpen.isEmpty()) {
                playSounds(p, def.firearmSoundOpen);
            }
            if (def.firearmSoundClose != null && !def.firearmSoundClose.isEmpty() && def.firearmOpenDurationTicks > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() { playSounds(p, def.firearmSoundClose); }
                }, def.firearmOpenDurationTicks);
            }
        }
        // Burstfire: queue next shots using reduced delay (ticks converted in manager)
        if (def.burstEnable && def.shotsPerBurst > 1 && def.delayBetweenShotsInBurstTicks > 0) {
            for (int i = 1; i < def.shotsPerBurst; i++) {
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        // Simulate another shot without re-checking everything heavy
                        PlayerInteractEvent fake = new PlayerInteractEvent(p, a, p.getItemInHand(), null, null);
                        onInteract(fake);
                    }
                }, def.delayBetweenShotsInBurstTicks * i);
            }
        }
        // Apply post-reload shoot delay if configured and we just finished a reload
        nextShotAt.put(p.getName(), now + delayMs);
        WeaponEvents.WeaponShootEvent shoot = new WeaponEvents.WeaponShootEvent(p, title, null);
        Bukkit.getPluginManager().callEvent(shoot);
    }

    private void applyCommonProjectileProps(Projectile proj, Player p, WeaponDefinition def, WeaponEvents.WeaponPreShootEvent pre) {
        // Apply spread
        org.bukkit.util.Vector v = proj.getVelocity();
        double spread = Math.toRadians(Math.max(0.0, pre.getBulletSpread()));
        v.setX(v.getX() + (random.nextDouble() - 0.5) * spread);
        v.setY(v.getY() + (random.nextDouble() - 0.5) * spread);
        v.setZ(v.getZ() + (random.nextDouble() - 0.5) * spread);
        // Apply speed
        double speedMul = Math.max(0.1, def.speed);
        proj.setVelocity(v.multiply(speedMul));
        // Remove gravity if configured for supported types
        if (def.removeBulletDrop && (proj instanceof Snowball || proj instanceof Egg || proj instanceof Arrow)) {
            try {
                java.lang.reflect.Method m = proj.getClass().getMethod("setGravity", boolean.class);
                m.invoke(proj, false);
            } catch (Throwable ignored) {}
        }
        // Fire props
        if (def.projectileFlames) {
            proj.setFireTicks(100);
        }
        if (def.projectileIncendiaryEnable) {
            proj.setFireTicks(Math.max(100, def.projectileIncendiaryDurationTicks));
        }
        // Store damage in metadata via entity custom name
        proj.setCustomName("TG|" + plugin.getWeaponManager().getWeaponTitle(p.getItemInHand()) + "|" + def.damage);
        proj.setCustomNameVisible(false);
        // Delayed removal or drag
        if (def.removalOrDragDelayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    if (proj == null || proj.isDead()) return;
                    if (def.removalOrDragIsRemoval) {
                        proj.remove();
                    } else {
                        try {
                            proj.setVelocity(proj.getVelocity().multiply(0.2));
                        } catch (Throwable ignored) {}
                    }
                }
            }, def.removalOrDragDelayTicks);
        }
    }

    private void scheduleGrenadeOrFlare(Player p, WeaponDefinition def, org.bukkit.entity.Item drop, boolean grenade) {
        int fuse = Math.max(20, 60); // default ~3s
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (drop == null || drop.isDead()) return;
                Location loc = drop.getLocation();
                drop.remove();
                if (grenade) {
                    // Use Explosions module if enabled
                    if (def.explosionsEnable && def.explosionRadius > 0.0) {
                        loc.getWorld().createExplosion(
                                loc.getX(), loc.getY(), loc.getZ(),
                                (float) def.explosionRadius,
                                def.explosionIncendiary,
                                !def.explosionNoGrief
                        );
                    } else {
                        loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 2.0f, false, false);
                    }
                } else {
                    // Flare: simple airstrike imitation as extra explosions along a line
                    org.bukkit.util.Vector dir = p.getLocation().getDirection().normalize();
                    for (int i = 0; i < 5; i++) {
                        Location l = loc.clone().add(dir.multiply(i * 2.5));
                        l.getWorld().createExplosion(l.getX(), l.getY(), l.getZ(), 2.0f, false, false);
                    }
                }
            }
        }, fuse);
    }

    private boolean genMatches(String key, int gen) {
        Integer cur = reloadGeneration.get(key);
        return cur != null && cur.intValue() == gen;
    }

    private void handleEnergyShot(Player p, WeaponDefinition def) {
        // Simple cuboid sweep in front of the player, damaging up to victims and penetrating walls
        int range = Math.max(1, def.energyRange);
        int radius = Math.max(0, def.energyRadius);
        int remainingWalls = def.energyWalls;
        int remainingVictims = def.energyVictims;
        org.bukkit.util.Vector dir = p.getLocation().getDirection().normalize();
        Location cursor = p.getEyeLocation().clone();
        int stepCount = range * 2; // 0.5 block steps
        for (int i = 0; i < stepCount && (remainingVictims > 0); i++) {
            cursor.add(dir.clone().multiply(0.5));
            // Wall check
            if (remainingWalls != Integer.MAX_VALUE) {
                org.bukkit.block.Block b = cursor.getBlock();
                if (b != null && b.getType().isSolid()) {
                    if (remainingWalls <= 0) break;
                    remainingWalls--;
                    // skip to next step
                    continue;
                }
            }
            // Damage entities in a small cube
            double r = Math.max(0.25, radius);
            for (Entity e : cursor.getWorld().getNearbyEntities(cursor, r, r, r)) {
                if (e instanceof LivingEntity && e != p) {
                    LivingEntity victim = (LivingEntity) e;
                    WeaponEvents.WeaponDamageEntityEvent we = new WeaponEvents.WeaponDamageEntityEvent(p, plugin.getWeaponManager().getWeaponTitle(p.getItemInHand()), victim, p, def.damage);
                    Bukkit.getPluginManager().callEvent(we);
                    if (!we.isCancelled()) {
                        victim.damage(Math.max(0.0, we.getDamage()), p);
                        remainingVictims = (remainingVictims == Integer.MAX_VALUE) ? Integer.MAX_VALUE : remainingVictims - 1;
                        if (remainingVictims == 0) break;
                    }
                }
            }
        }
        // Visual: light smoke trail
        try { p.getWorld().playEffect(p.getLocation(), org.bukkit.Effect.SMOKE, 4); } catch (Exception ignored) {}
    }

    @EventHandler
    public void onHit(ProjectileHitEvent e) {
        Entity shooter = null;
        if (e.getEntity().getShooter() instanceof Entity) shooter = (Entity) e.getEntity().getShooter();
        if (!(shooter instanceof Player)) return;
        Player p = (Player) shooter;
        ItemStack inHand = p.getItemInHand();
        String title = plugin.getWeaponManager().getWeaponTitle(inHand);
        if (title == null) return;
        WeaponEvents.WeaponHitBlockEvent hit = new WeaponEvents.WeaponHitBlockEvent(p, title, e.getEntity());
        Bukkit.getPluginManager().callEvent(hit);

        // Explosions on impact
        WeaponDefinition def = plugin.getWeaponManager().getDefinition(title);
        if (def != null && def.explosionsEnable && def.explosionRadius > 0.0) {
            Location loc = e.getEntity().getLocation();
            // createExplosion(power): power roughly equals radius
            loc.getWorld().createExplosion(
                    loc.getX(), loc.getY(), loc.getZ(),
                    (float) def.explosionRadius,
                    def.explosionIncendiary,
                    !def.explosionNoGrief
            );
        }
        // Remove arrows on impact if configured
        if (def != null && def.removeArrowsOnImpact && e.getEntity() instanceof Arrow) {
            try { e.getEntity().remove(); } catch (Throwable ignored) {}
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Projectile)) return;
        Projectile proj = (Projectile) e.getDamager();
        if (!(proj.getShooter() instanceof Player)) return;
        Player p = (Player) proj.getShooter();
        String meta = proj.getCustomName();
        String title = null;
        double dmg = e.getDamage();
        if (meta != null && meta.startsWith("TG|")) {
            String[] parts = meta.split("\\|");
            if (parts.length >= 3) {
                title = parts[1];
                try { dmg = Double.parseDouble(parts[2]); } catch (Exception ignored) {}
            }
        }
        if (title == null) title = plugin.getWeaponManager().getWeaponTitle(p.getItemInHand());
        // Headshot detection (rough): compare projectile Y to victim eye level
        boolean headshot = false;
        if (e.getEntity() instanceof LivingEntity) {
            LivingEntity victim = (LivingEntity) e.getEntity();
            double projY = proj.getLocation().getY();
            double eyeY = victim.getEyeLocation().getY();
            if (Math.abs(projY - eyeY) <= 0.35) {
                headshot = true;
            }
        }
        WeaponDefinition def = plugin.getWeaponManager().getDefinition(title);
        if (def != null && def.headshotEnable && headshot) {
            dmg = Math.max(0.0, dmg + def.headshotBonusDamage);
        }
        WeaponEvents.WeaponDamageEntityEvent we = new WeaponEvents.WeaponDamageEntityEvent(p, title, e.getEntity(), proj, dmg);
        Bukkit.getPluginManager().callEvent(we);
        if (we.isCancelled()) { e.setCancelled(true); return; }
        e.setDamage(we.getDamage());
        // Abilities: knockback on hit
        if (def.abilityKnockback > 0 && e.getEntity() instanceof LivingEntity) {
            LivingEntity victim = (LivingEntity) e.getEntity();
            org.bukkit.util.Vector kb = victim.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(def.abilityKnockback / 10.0);
            victim.setVelocity(victim.getVelocity().add(kb));
        }
        // Abilities: reset hit cooldown (reduce noDamageTicks)
        if (def.abilityResetHitCooldown && e.getEntity() instanceof LivingEntity) {
            LivingEntity victim = (LivingEntity) e.getEntity();
            victim.setNoDamageTicks(0);
        }
    }

    private void beginReload(Player player, WeaponDefinition def, String key, long now) {
        if (!def.reloadEnable || def.reloadAmount <= 0) return;
        int currentMag = magazine.containsKey(key) ? Math.max(0, magazine.get(key)) : 0;
        int capacity = Math.max(0, def.reloadAmount);
        int need = Math.max(0, capacity - currentMag);
        if (need <= 0) return;

        // Determine how many bullets we can actually load based on ammo rules
        int bulletsToLoad = need;
        if (def.ammoEnable && def.ammoItemId != null && !def.ammoItemId.isEmpty()) {
            int available = countAmmoItems(player.getInventory(), def);
            if (def.takeAmmoAsMagazine) {
                if (available >= 1) {
                    consumeAmmoItems(player.getInventory(), def, 1);
                    bulletsToLoad = need; // full mag
                } else {
                    bulletsToLoad = 0;
                }
            } else if (def.takeAmmoOnReload) {
                int perBullet = Math.max(1, def.ammoTakePerShot);
                int maxByAmmo = available / perBullet;
                bulletsToLoad = Math.min(need, Math.max(0, maxByAmmo));
                // For individual reload we will consume per bullet; otherwise consume upfront
                if (!def.reloadBulletsIndividually && bulletsToLoad > 0) {
                    consumeAmmoItems(player.getInventory(), def, bulletsToLoad * perBullet);
                }
            }
        }

        if (def.destroyWhenEmpty && bulletsToLoad <= 0) {
            player.setItemInHand(null);
            return;
        }

        if (bulletsToLoad <= 0) {
            if (def.reloadSoundsOutOfAmmo != null && !def.reloadSoundsOutOfAmmo.isEmpty()) {
                playSounds(player, def.reloadSoundsOutOfAmmo);
            } else {
                playSounds(player, def.soundsOutOfAmmo);
            }
            return;
        }

        // Capture for inner classes
        final int bulletsToLoadFinal = bulletsToLoad;

        // Time-based reload
        if (def.reloadDurationTicks > 0L) {
            int openDelayTicks = Math.max(0, def.firearmReloadOpenDelayTicks);
            int closeDelayTicks = Math.max(0, def.firearmReloadCloseDelayTicks);
            // Firearm action module adjustments
            boolean ignoreAction = def.dualWield; // Dual wield ignores Firearm_Action per spec
            boolean isPump = def.firearmActionType == org.lmvictor20.toperguns.weapon.WeaponDefinition.FirearmActionType.PUMP;
            boolean isBreakOrRevolver = def.firearmActionType == org.lmvictor20.toperguns.weapon.WeaponDefinition.FirearmActionType.BREAK
                    || def.firearmActionType == org.lmvictor20.toperguns.weapon.WeaponDefinition.FirearmActionType.REVOLVER;
            if (ignoreAction) {
                openDelayTicks = 0;
                closeDelayTicks = 0;
            } else if (isPump) {
                // Pump-action does not need to be opened to reload
                openDelayTicks = 0;
            } else if (isBreakOrRevolver) {
                // Before reload begins, perform firearm open action duration and sound
                openDelayTicks = Math.max(0, def.firearmOpenDurationTicks) + openDelayTicks;
            }
            final int openDelayTicksFinal = openDelayTicks;
            final int closeDelayTicksFinal = closeDelayTicks;
            // Start new reload generation
            int gen = (reloadGeneration.containsKey(key) ? reloadGeneration.get(key) : 0) + 1;
            reloadGeneration.put(key, gen);

            long totalTicks;
            if (def.reloadBulletsIndividually) {
                long perBullet = Math.max(1L, def.reloadDurationTicks);
                totalTicks = openDelayTicksFinal + (perBullet * Math.max(1, bulletsToLoadFinal)) + closeDelayTicksFinal;
            } else {
                totalTicks = openDelayTicksFinal + Math.max(0, def.reloadDurationTicks) + closeDelayTicksFinal;
            }
            reloadingUntil.put(key, now + totalTicks * 50L);
            // Play action open sound immediately if present and not ignored
            if (!ignoreAction && openDelayTicksFinal > 0 && def.firearmSoundOpen != null && !def.firearmSoundOpen.isEmpty()) {
                playSounds(player, def.firearmSoundOpen);
            }
            // Mark BREAK/REVOLVER as open during reload
            if (!ignoreAction && (def.firearmActionType == org.lmvictor20.toperguns.weapon.WeaponDefinition.FirearmActionType.BREAK
                    || def.firearmActionType == org.lmvictor20.toperguns.weapon.WeaponDefinition.FirearmActionType.REVOLVER)) {
                actionOpen.put(key, true);
            }
            // Play reloading sounds when actual reloading starts (after open delay)
            if (!def.reloadBulletsIndividually) {
                if (def.soundsReloading != null && !def.soundsReloading.isEmpty()) {
                if (openDelayTicksFinal <= 0) {
                    playSounds(player, def.soundsReloading);
                } else {
                    final int genAtSchedule = gen;
                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (!genMatches(key, genAtSchedule)) return;
                            playSounds(player, def.soundsReloading);
                        }
                    }, openDelayTicksFinal);
                }
                }
            }

            if (def.reloadBulletsIndividually) {
                final int totalSteps = bulletsToLoadFinal;
                final int startMag = currentMag;
                final int capacityFinal = capacity;
                final String keyFinal = key;
                final int perBulletCost = Math.max(1, def.ammoTakePerShot);
                final boolean consumePerBullet = def.ammoEnable && def.takeAmmoOnReload && def.ammoItemId != null && !def.ammoItemId.isEmpty();
                final int period = Math.max(1, (int) Math.max(1L, def.reloadDurationTicks));
                final int bulletsToLoadFinalLocal = totalSteps;
                final int genFinal = gen;
                final Player playerRef = player;
                new org.bukkit.scheduler.BukkitRunnable() {
                    int loaded = 0;
                    @Override
                    public void run() {
                        if (!genMatches(keyFinal, genFinal)) { this.cancel(); return; }
                        // If предмет не в руке — прервать перезарядку
                        if (playerRef.getItemInHand() == null || playerRef.getItemInHand().getType() == Material.AIR
                                || !keyFinal.equalsIgnoreCase(playerRef.getName().toLowerCase(Locale.ROOT) + "|" + (plugin.getWeaponManager().getWeaponTitle(playerRef.getItemInHand()) == null ? "" : plugin.getWeaponManager().getWeaponTitle(playerRef.getItemInHand()).toLowerCase(Locale.ROOT)))) {
                            this.cancel();
                            return;
                        }
                        if (loaded >= bulletsToLoadFinalLocal) {
                            // Defer close sound and finalization by closeDelayTicks if any
                            Runnable complete = new Runnable() {
                                @Override
                                public void run() {
                                    if (!genMatches(keyFinal, genFinal)) return;
                                    if (!ignoreAction && def.firearmSoundClose != null && !def.firearmSoundClose.isEmpty()) {
                                        playSounds(player, def.firearmSoundClose);
                                    }
                                    updateHeldItemDisplay(player, def, Math.min(capacityFinal, startMag + loaded));
                                    long postDelayTicks = Math.max(0L, def.reloadShootDelayTicks) + Math.max(0, def.firearmCloseShootDelayTicks);
                                    if (postDelayTicks > 0) {
                                        long extra = postDelayTicks * 50L;
                                        nextShotAt.put(player.getName(), System.currentTimeMillis() + extra);
                                    }
                                    // Close state finished
                                    actionOpen.put(keyFinal, false);
                                }
                            };
                            if (closeDelayTicksFinal > 0) {
                                Bukkit.getScheduler().runTaskLater(plugin, complete, closeDelayTicksFinal);
                            } else {
                                complete.run();
                            }
                            this.cancel();
                            return;
                        }
                        if (consumePerBullet) {
                            if (!consumeAmmoItems(player.getInventory(), def, perBulletCost)) {
                                playSounds(player, def.reloadSoundsOutOfAmmo);
                                this.cancel();
                                return;
                            }
                        }
                        loaded++;
                        if (def.soundsReloading != null && !def.soundsReloading.isEmpty()) {
                            playSounds(player, def.soundsReloading);
                        }
                        int cur = Math.min(capacityFinal, startMag + loaded);
                        magazine.put(keyFinal, cur);
                        // During reload show «N»ʳ; keep the item metadata updated
                        updateHeldItemDisplayReloading(playerRef, def, cur);
                    }
                }.runTaskTimer(plugin, openDelayTicksFinal, period);
            } else {
                // Full reload completes at end
                // Schedule reloading start sound handled above; completion after open+duration+close
                final int genAtSchedule = gen;
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (!genMatches(key, genAtSchedule)) return;
                        int newMag = Math.min(capacity, currentMag + bulletsToLoadFinal);
                        magazine.put(key, newMag);
                        if (!ignoreAction && def.firearmSoundClose != null && !def.firearmSoundClose.isEmpty()) {
                            playSounds(player, def.firearmSoundClose);
                        }
                        updateHeldItemDisplay(player, def, newMag);
                        long postDelayTicks = Math.max(0L, def.reloadShootDelayTicks) + Math.max(0, def.firearmCloseShootDelayTicks);
                        if (postDelayTicks > 0) {
                            long extra = postDelayTicks * 50L;
                            nextShotAt.put(player.getName(), System.currentTimeMillis() + extra);
                        }
                        // Close state finished
                        actionOpen.put(key, false);
                    }
                }, openDelayTicksFinal + Math.max(0, def.reloadDurationTicks) + closeDelayTicksFinal);
            }
        } else {
            // Instant reload
            int newMag = Math.min(capacity, currentMag + bulletsToLoad);
            magazine.put(key, newMag);
            updateHeldItemDisplay(player, def, newMag);
        }
    }

    private void updateHeldItemDisplayReloading(Player player, WeaponDefinition def, int current) {
        ItemStack hand = player.getItemInHand();
        if (hand == null) return;
        org.bukkit.inventory.meta.ItemMeta meta = hand.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        String colored = org.bukkit.ChatColor.translateAlternateColorCodes('&', def.display);
        String base = org.bukkit.ChatColor.stripColor(colored);
        int idx = base.lastIndexOf('«');
        if (idx > 0 && base.endsWith("»")) {
            base = base.substring(0, idx).trim();
            int idxColor = colored.lastIndexOf('«');
            if (idxColor > 0 && colored.endsWith("»")) {
                colored = colored.substring(0, idxColor).trim();
            }
        }
        int n = Math.max(0, current);
        // Add superscript small 'r' after » while reloading. Using \u02B3 (modifier letter r)
        String withAmmo = colored + " \u00AB" + n + "\u00BB\u02B3";
        meta.setDisplayName(withAmmo);
        hand.setItemMeta(meta);
        player.setItemInHand(hand);
    }

    private boolean canReloadWithAmmo(PlayerInventory inv, WeaponDefinition def) {
        if (!def.ammoEnable || def.ammoItemId == null || def.ammoItemId.isEmpty()) return true;
        int available = countAmmoItems(inv, def);
        if (def.takeAmmoAsMagazine) {
            return available >= 1;
        }
        int perBullet = Math.max(1, def.ammoTakePerShot);
        return available >= perBullet;
    }

    private int countAmmoItems(PlayerInventory inv, WeaponDefinition def) {
        Material targetMat = def.ammoItemMaterial != null ? def.ammoItemMaterial : (def.ammoItemId != null ? Material.matchMaterial(def.ammoItemId) : null);
        if (targetMat == null) return 0;
        int dataReq = def.ammoItemData;
        String nameSub = def.ammoNameCheck;
        int count = 0;
        for (ItemStack it : inv.getContents()) {
            if (matchesAmmo(it, targetMat, dataReq, nameSub)) count += it.getAmount();
        }
        return count;
    }

    private boolean consumeAmmoItems(PlayerInventory inv, WeaponDefinition def, int amount) {
        Material targetMat = def.ammoItemMaterial != null ? def.ammoItemMaterial : (def.ammoItemId != null ? Material.matchMaterial(def.ammoItemId) : null);
        if (targetMat == null) return false;
        int dataReq = def.ammoItemData;
        String nameSub = def.ammoNameCheck;
        int remaining = amount;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack it = contents[i];
            if (!matchesAmmo(it, targetMat, dataReq, nameSub)) continue;
            int take = Math.min(remaining, it.getAmount());
            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) contents[i] = null;
            remaining -= take;
        }
        inv.setContents(contents);
        return remaining <= 0;
    }

    private boolean matchesAmmo(ItemStack it, Material targetMat, int dataReq, String nameSub) {
        if (it == null) return false;
        if (it.getType() != targetMat) return false;
        if (dataReq >= 0) {
            try {
                if (it.getDurability() != (short) dataReq) return false;
            } catch (Throwable ignored) {}
        }
        if (nameSub != null && !nameSub.isEmpty()) {
            org.bukkit.inventory.meta.ItemMeta m = it.getItemMeta();
            if (m == null || !m.hasDisplayName()) return false;
            String disp = m.getDisplayName();
            // Accept if contains raw or strip-color match
            String strippedDisp = org.bukkit.ChatColor.stripColor(disp);
            String strippedSub = org.bukkit.ChatColor.stripColor(nameSub);
            return disp.contains(nameSub) || strippedDisp.contains(strippedSub);
        }
        return true;
    }

    private void playSounds(Player player, java.util.List<String> names) {
        if (names == null) return;
        for (String name : names) {
            if (name == null || name.isEmpty()) continue;
            String upper = name.toUpperCase(Locale.ROOT);
            String[] parts = upper.split("-");
            String soundName = parts.length > 0 ? parts[0] : upper;
            float vol = 1.0f;
            float pitch = 1.0f;
            // NAME-VOL-PITCH-(optional delay ignored)
            if (parts.length >= 2) {
                try { vol = Float.parseFloat(parts[1]); } catch (Exception ignored) {}
            }
            if (parts.length >= 3) {
                try { pitch = Float.parseFloat(parts[2]); } catch (Exception ignored) {}
            }
            Sound resolved = resolveSoundCompat(soundName);
            if (resolved != null) {
                player.getWorld().playSound(player.getLocation(), resolved, vol, pitch);
            }
        }
    }

    // Attempt to resolve legacy CrackShot sound names to modern Bukkit enum names
    private Sound resolveSoundCompat(String upperName) {
        if (upperName == null || upperName.isEmpty()) return null;
        try {
            return Sound.valueOf(upperName);
        } catch (IllegalArgumentException ignored) {}

        // Common legacy → modern aliases
        String candidate = null;
        if ("GHAST_FIREBALL".equals(upperName)) {
            // 1.9+ renamed
            String[] fallback = new String[] { "GHAST_SHOOT", "ENTITY_GHAST_SHOOT" };
            for (String f : fallback) {
                try { return Sound.valueOf(f); } catch (IllegalArgumentException ignored) {}
            }
        } else if ("CHICKEN_EGG_POP".equals(upperName)) {
            String[] fallback = new String[] { "ENTITY_EGG_THROW", "ENTITY_CHICKEN_EGG" };
            for (String f : fallback) {
                try { return Sound.valueOf(f); } catch (IllegalArgumentException ignored) {}
            }
        } else if ("EXPLODE".equals(upperName) || upperName.endsWith("_EXPLODE")) {
            String[] fallback = new String[] { "ENTITY_GENERIC_EXPLODE", "EXPLODE", "EXPLOSION" };
            for (String f : fallback) {
                try { return Sound.valueOf(f); } catch (IllegalArgumentException ignored) {}
            }
        }

        // Heuristic: try with common prefixes
        String[] prefixes = new String[] { "ENTITY_", "BLOCK_", "ITEM_" };
        for (String pfx : prefixes) {
            try { return Sound.valueOf(pfx + upperName); } catch (IllegalArgumentException ignored) {}
        }

        // Heuristic: attempt simple replacements
        candidate = upperName.replace("FIREBALL", "SHOOT");
        try { return Sound.valueOf(candidate); } catch (IllegalArgumentException ignored) {}
        for (String pfx : prefixes) {
            try { return Sound.valueOf(pfx + candidate); } catch (IllegalArgumentException ignored) {}
        }

        // Fallback: contains-match search
        for (Sound s : Sound.values()) {
            String n = s.name();
            if (n.contains(upperName) || n.contains(candidate)) {
                return s;
            }
        }
        return null;
    }

    private void updateHeldItemDisplay(Player player, WeaponDefinition def, int current) {
        ItemStack hand = player.getItemInHand();
        if (hand == null) return;
        org.bukkit.inventory.meta.ItemMeta meta = hand.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        // Base on configured display; strip any existing suffix ([], <>, (), {}, «»)
        String colored = org.bukkit.ChatColor.translateAlternateColorCodes('&', def.display);
        String base = org.bukkit.ChatColor.stripColor(colored);
        char[][] pairs = new char[][] { {'[', ']'}, {'<', '>'}, {'(', ')'}, {'{', '}'}, {'\u00AB', '\u00BB'} };
        int cutAt = -1;
        int cutAtColored = -1;
        for (char[] p : pairs) {
            int idxOpen = base.lastIndexOf(p[0]);
            int idxClose = base.lastIndexOf(p[1]);
            if (idxOpen >= 0 && idxClose > idxOpen) cutAt = Math.max(cutAt, idxOpen);
            int idxOpenC = colored.lastIndexOf(p[0]);
            int idxCloseC = colored.lastIndexOf(p[1]);
            if (idxOpenC >= 0 && idxCloseC > idxOpenC) cutAtColored = Math.max(cutAtColored, idxOpenC);
        }
        if (cutAt > 0) base = base.substring(0, cutAt).trim();
        if (cutAtColored > 0) colored = colored.substring(0, cutAtColored).trim();

        // Always render as guillemets: «N»
        int n = Math.max(0, current);
        String withAmmo = colored + " \u00AB" + n + "\u00BB";
        meta.setDisplayName(withAmmo);
        hand.setItemMeta(meta);
        player.setItemInHand(hand);
    }

    private int inferCurrentAmmoFromItem(ItemStack item, WeaponDefinition def) {
        try {
            if (item == null) return Math.max(0, def.reloadStartingAmount > 0 ? def.reloadStartingAmount : def.reloadAmount);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return Math.max(0, def.reloadStartingAmount > 0 ? def.reloadStartingAmount : def.reloadAmount);
            String disp = org.bukkit.ChatColor.stripColor(meta.getDisplayName());
            // Support suffix formats: [], <>, (), {}, «» and optional trailing superscript chars; also optional /M
            int n = -1;
            char[][] pairs = new char[][] { {'[', ']'}, {'<', '>'}, {'(', ')'}, {'{', '}'}, {'\u00AB', '\u00BB'} };
            for (char[] p : pairs) {
                int idxOpen = disp.lastIndexOf(p[0]);
                int idxClose = disp.lastIndexOf(p[1]);
                if (idxOpen >= 0 && idxClose > idxOpen) {
                    String inner = disp.substring(idxOpen + 1, idxClose);
                    if (inner.contains("/")) inner = inner.split("/", 2)[0];
                    try { n = Integer.parseInt(inner.trim()); } catch (Exception ignored) {}
                    if (n >= 0) break;
                }
            }
            if (n >= 0) return Math.max(0, Math.min(def.reloadAmount > 0 ? def.reloadAmount : Integer.MAX_VALUE, n));
        } catch (Exception ignored) {}
        // Fallback: starting or full magazine
        int start = Math.max(0, def.reloadStartingAmount);
        if (start == 0 && def.reloadAmount > 0) start = def.reloadAmount;
        return start;
    }

    // Clear fall-damage immunity when переключают предмет
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeldChange(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        ItemStack newItem = p.getInventory().getItem(e.getNewSlot());
        String title = plugin.getWeaponManager().getWeaponTitle(newItem);
        WeaponDefinition def = title == null ? null : plugin.getWeaponManager().getDefinition(title);
        if (title != null && def != null) {
            currentWeaponByPlayer.put(p.getName(), title);
        } else {
            currentWeaponByPlayer.remove(p.getName());
        }
        if (def == null || !def.abilityNoFallDamage) {
            // Only cancel no-fall if джампер не в руке
            p.setFallDistance(0f); // reset accumulated, but damage rules apply next fall
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDropReload(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        // Allow dropping via inventory drag: if cursor holds an item, treat as GUI drop
        try {
            ItemStack cursor = p.getItemOnCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                return;
            }
        } catch (Throwable ignored) {}
        ItemStack inHand = p.getItemInHand();
        ItemStack dropped = null;
        try { if (e.getItemDrop() != null) dropped = e.getItemDrop().getItemStack(); } catch (Exception ignored) {}
        String titleFromHand = plugin.getWeaponManager().getWeaponTitle(inHand);
        String titleFromDropped = plugin.getWeaponManager().getWeaponTitle(dropped);
        String currentTitle = titleFromHand != null ? titleFromHand : currentWeaponByPlayer.get(p.getName());
        // Allow dropping items via inventory drag unless the dropped item is exactly the currently held weapon
        if (currentTitle == null || titleFromDropped == null || !titleFromDropped.equalsIgnoreCase(currentTitle)) return;
        WeaponDefinition def = plugin.getWeaponManager().getDefinition(currentTitle);
        if (def == null || !def.reloadEnable || def.reloadAmount <= 0) return;
        // If reload with mouse is enabled, don't consume drop for reload
        if (def.reloadWithMouse) return;
        // Pre-cancel drop immediately for our weapon to prevent other plugins from letting it through
        e.setCancelled(true);
        try { if (e.getItemDrop() != null && !e.getItemDrop().isDead()) e.getItemDrop().remove(); } catch (Exception ignored) {}
        try { p.updateInventory(); } catch (Throwable ignored) {}
        // If ammo is required as magazine or on reload and none present, play out of ammo
        if (def.ammoEnable && (def.takeAmmoOnReload || def.takeAmmoAsMagazine) && def.ammoItemId != null && !def.ammoItemId.isEmpty()) {
            if (!canReloadWithAmmo(p.getInventory(), def)) {
                if (def.reloadSoundsOutOfAmmo != null && !def.reloadSoundsOutOfAmmo.isEmpty()) {
                    playSounds(p, def.reloadSoundsOutOfAmmo);
                } else {
                    playSounds(p, def.soundsOutOfAmmo);
                }
                return;
            }
        }
        String key = p.getName().toLowerCase(Locale.ROOT) + "|" + currentTitle.toLowerCase(Locale.ROOT);
        // Ensure magazine initialized so partial mags (e.g., 46/50) are respected
        if (!magazine.containsKey(key)) {
            ItemStack basis = (inHand != null && inHand.getType() != Material.AIR) ? inHand : dropped;
            int start = inferCurrentAmmoFromItem(basis, def);
            magazine.put(key, start);
            updateHeldItemDisplay(p, def, start);
        }
        beginReload(p, def, key, System.currentTimeMillis());
        // Schedule one more inventory fix next tick to out-prioritize other listeners
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                try { if (e.getItemDrop() != null && !e.getItemDrop().isDead()) e.getItemDrop().remove(); } catch (Exception ignored) {}
                try { p.updateInventory(); } catch (Throwable ignored) {}
            }
        });
    }

    // Mouse-based reload: when enabled, use the shooting click to trigger reload if empty
    // and skip drop-based reload. This integrates with the main interact handler above.

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFall(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        Player p = (Player) e.getEntity();
        ItemStack inHand = p.getItemInHand();
        if (inHand == null || inHand.getType() == Material.AIR) return;
        String title = plugin.getWeaponManager().getWeaponTitle(inHand);
        if (title == null) return;
        WeaponDefinition def = plugin.getWeaponManager().getDefinition(title);
        if (def != null && def.abilityNoFallDamage) {
            e.setCancelled(true);
            p.setFallDistance(0f);
        }
    }
}


