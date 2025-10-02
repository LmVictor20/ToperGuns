## ToperGuns API Guide

This document describes how to interact with the ToperGuns plugin from your own Bukkit/Spigot/Paper plugins. It covers setup, the `ToperGunsPlugin` entrypoint, the `WeaponManager` API, emitted events, and command/config integration.

### Requirements
- Server: Spigot or Paper (tested with modern Bukkit APIs).
- Dependency: Add the built ToperGuns JAR to your plugin's build path, or install it to your local Maven repository and depend on it. Package names live under `org.lmvictor20.toperguns`.

### Getting the plugin instance
- Static accessor: `ToperGunsPlugin.getInstance()`
- From Bukkit: `Bukkit.getPluginManager().getPlugin("ToperGuns")` and cast if needed.

```java
import org.lmvictor20.toperguns.ToperGunsPlugin;

ToperGunsPlugin tg = ToperGunsPlugin.getInstance();
```

## WeaponManager
The `WeaponManager` loads, parses, and provides access to weapon definitions and their item forms.

Get it:
```java
WeaponManager wm = ToperGunsPlugin.getInstance().getWeaponManager();
```

### Methods
- `void reload()`
  - Reloads `plugins/ToperGuns/weapons.yml` and any additional `plugins/ToperGuns/weapons/*.yml` files. Rebuilds internal caches.

- `org.bukkit.inventory.ItemStack generateWeapon(String weaponTitle)`
  - Returns a fresh `ItemStack` clone for the configured weapon key (case-insensitive). Returns `null` if unknown.
  - The display name and lore are applied as configured. Dynamic ammo suffixes are not pre-applied; they are maintained during gameplay by the listener.

- `String getWeaponTitle(org.bukkit.inventory.ItemStack item)`
  - Infers the weapon key from an item in hand, matching by configured display name (color codes ignored). Also strips dynamic suffixes like «N», `[N]`, `<N>`, `(N)`, `{N}` with optional superscript reload markers.
  - Returns `null` if the item does not match any known weapon.

- `WeaponDefinition getDefinition(String title)`
  - Returns the parsed, immutable definition object for a weapon key, or `null` if none.

### Notes
- Titles/keys are case-insensitive internally.
- Materials and sounds support some legacy compatibility/mapping, but should match your server version whenever possible.
- The manager supports two config styles:
  - CrackShot-like per-weapon roots (preferred)
  - Legacy `weapons:` simple format (backward-compat)

## Events
All events live under `org.lmvictor20.toperguns.event.WeaponEvents` and extend a `Base` class that implements `org.bukkit.event.Cancellable`. Subscribe via standard Bukkit `@EventHandler`.

### Currently emitted
- `WeaponPrepareShootEvent(Player player, String weaponTitle)`
  - Fired early in the click handler. Cancelling prevents shooting and cancels interaction.

- `WeaponPreShootEvent(Player player, String weaponTitle, double bulletSpread)`
  - Fired right before projectiles are spawned. You may adjust spread via `setBulletSpread(double)`; cancelling prevents shooting.

- `WeaponShootEvent(Player player, String weaponTitle, Entity projectile)`
  - Fired when a shot occurs. `projectile` may be `null` for some modes or queued shots.

- `WeaponHitBlockEvent(Player player, String weaponTitle, Entity projectile)`
  - Fired on `ProjectileHitEvent`. Useful for custom impact behavior. Not all projectile types provide a block reference.

- `WeaponDamageEntityEvent(Player player, String weaponTitle, Entity victim, Entity damager, double damage)`
  - Fired during `EntityDamageByEntityEvent` for weapon projectiles. You can `setDamage` to modify final damage or cancel to prevent it.
  - Headshots are detected heuristically (projectile Y vs eye Y). If `Headshot.Enable` is true, bonus damage is auto-applied before the event.

### Declared but not currently emitted
- `WeaponExplodeEvent`, `WeaponReloadEvent`, `WeaponReloadCompleteEvent`, `WeaponScopeEvent`, `WeaponPlaceMineEvent`, `WeaponTriggerEvent`, `WeaponFireRateEvent` are present in API but not fired by the current listener implementation. They are reserved for future expansion.

### Example listener
```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.lmvictor20.toperguns.event.WeaponEvents;

public final class GunsListener implements Listener {
  @EventHandler
  public void onPrepare(WeaponEvents.WeaponPrepareShootEvent e) {
    // Block shooting in a safe region
    boolean inSafe = /* your region check */ false;
    if (inSafe) e.setCancelled(true);
  }

  @EventHandler
  public void onPreShoot(WeaponEvents.WeaponPreShootEvent e) {
    // Reduce spread for VIPs
    if (e.getPlayer().hasPermission("vip")) {
      e.setBulletSpread(Math.max(0.0, e.getBulletSpread() * 0.6));
    }
  }

  @EventHandler
  public void onDamage(WeaponEvents.WeaponDamageEntityEvent e) {
    // Scale damage in arenas
    double scaled = e.getDamage() * 0.85;
    e.setDamage(scaled);
  }
}
```

## Commands
- `giveweapon` — `/giveweapon <player> <weaponTitle> [amount]`
  - Spawns configured weapon items and adds to target inventory.

## Configuration integration
- Main file: `plugins/ToperGuns/weapons.yml` (see defaults in the JAR under `src/main/resources`).
- Extra files: any `*.yml` under `plugins/ToperGuns/weapons/` are also parsed (CrackShot-like structure).
- Reload programmatically:
```java
ToperGunsPlugin.getInstance().getWeaponManager().reload();
```

### Selected modules (subset)
- `Item_Information`: `Item_Name`, `Item_Type`, `Item_Lore`
- `Shooting`: `Projectile_Type`, `Projectile_Speed`, `Projectile_Damage`, `Bullet_Spread`, `Delay_Between_Shots`, `Right_Click_To_Shoot`, `Cancel_*`, `Recoil_Amount`, `Sounds_Shoot`, `Dual_Wield`, `Remove_Bullet_Drop`, `Removal_Or_Drag_Delay`
- `Fully_Automatic`: `Enable`, `Fire_Rate`
- `Ammo`: `Enable`, `Ammo_Item_ID`, `Take_Ammo_Per_Shot`, optional `Ammo_Name_Check`, sounds
- `Reload`: `Enable`, `Reload_Amount`, `Starting_Amount`, `Reload_Duration`, `Reload_With_Mouse`, `Reload_Bullets_Individually`, `Take_Ammo_On_Reload`, `Take_Ammo_As_Magazine`, sounds
- `Scope`: `Enable`, `Zoom_Amount`, `Zoom_Bullet_Spread`
- `Sneak`: `Enable`, `Bullet_Spread`
- `Headshot`: `Enable`, `Bonus_Damage`
- `Explosions`: `Enable`, `Explosion_Radius`, `Explosion_Incendiary`, `Explosion_No_Grief`
- `Firearm_Action`: `Type (NONE|SLIDE|BOLT|LEVER|PUMP|BREAK|REVOLVER)`, open/close durations, sounds
- `Particles`: `Particle_Player_Shoot`

See `docs/modules_guide.md` and `src/main/resources/weapons.template.yml` for more details and examples.

## Best practices
- Prefer keys without spaces for weapon titles (e.g., `PISTOL`, `RIFLE`).
- Keep `Bullet_Spread` small; very high values create unpredictable trajectories.
- Use server-version-correct sound and material names to avoid silent failures.
- Avoid heavy logic in events that fire per-shot; offload to async tasks if possible (but never touch Bukkit API off-main thread).

## Troubleshooting
- Unknown weapon from `/giveweapon`: check the exact key in `weapons.yml` and ensure the file loaded (see console on startup or call `reload()`).
- No sounds: verify sound enum names for your server version; legacy aliases are attempted but not guaranteed.
- Ammo not consumed: ensure `Ammo.Enable` true and either `Take_Ammo_Per_Shot` or reload rules are set. For name-locked ammo, set `Ammo_Name_Check` to match the display name substring.


