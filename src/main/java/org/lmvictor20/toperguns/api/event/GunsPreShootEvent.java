package org.lmvictor20.toperguns.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class GunsPreShootEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled;
    private final Player player;
    private final String weaponId;
    private final ItemStack weaponItem;

    public GunsPreShootEvent(Player player, String weaponId, ItemStack weaponItem) {
        this.player = player;
        this.weaponId = weaponId;
        this.weaponItem = weaponItem;
    }

    public Player getPlayer() { return player; }
    public String getWeaponId() { return weaponId; }
    public ItemStack getWeaponItem() { return weaponItem; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}


