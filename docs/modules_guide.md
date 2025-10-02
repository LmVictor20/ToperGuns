ToperGuns + CrackShot-like Modules

This plugin supports a CrackShot-like configuration structure for weapons. It is not a full clone, but it aims to be familiar for server owners.

References:
- https://github.com/Shampaggon/CrackShot/wiki/List-of-Modules
- https://github.com/Shampaggon/CrackShot/wiki/The-Complete-Guide-to-CrackShot#modules

Supported modules (subset)

Item_Information:
- Item_Name: Display name (supports & color codes)
- Item_Type: Bukkit Material (e.g., IRON_HOE)
- Item_Lore: List of lore strings

Shooting:
- Right_Click_To_Shoot: true/false (false implies left-click)
- Cancel_Right_Click_Interactions: true/false
- Cancel_Left_Click_Block_Damage: true/false
- Projectile_Type: SNOWBALL | EGG | ARROW
- Projectile_Speed: double multiplier on velocity
- Projectile_Damage: double (applied on hit)
- Bullet_Spread: double (small numbers recommended)
- Delay_Between_Shots: milliseconds (used when Fully_Automatic disabled)
- Recoil_Amount: double (visual placeholder)
- Sounds_Shoot: list of Bukkit sounds

Fully_Automatic:
- Enable: true/false
- Fire_Rate: shots per second (overrides Delay_Between_Shots)

Ammo:
- Enable: true/false
- Ammo_Item_ID: Bukkit Material to consume (e.g., ARROW)
- Take_Ammo_Per_Shot: items consumed per shot

Reload:
- Enable: true/false
- Starting_Amount: initial magazine bullets
- Reload_Amount: magazine size
- Reload_Duration: ticks (20 = 1s)

Scope:
- Enable: true/false
- Zoom_Amount: informational only
- Zoom_Bullet_Spread: spread used while sneaking

Headshot:
- Enable: true/false
- Bonus_Damage: added on detected headshots (approximation)

Explosions:
- Enable: true/false
- Explosion_Radius: explosion power
- Explosion_Incendiary: fire
- Explosion_No_Grief: prevent terrain damage

Unsupported nodes are parsed but ignored and stored in `WeaponDefinition.rawModules` for potential future use.

Backward compatibility
- The old `weapons:` YAML (material/display/lore/projectile/damage/speed/spread/cooldown_ms/sound) still works.

Examples
- See `src/main/resources/weapons.yml` and `src/main/resources/weapons.template.yml`.

Notes
- Sound names and materials must match your server version.
- Spread is applied as a small random offset to projectile velocity.
- Recoil is minimal; you may extend `WeaponListener` to apply screen shake or knockback.

