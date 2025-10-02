package org.lmvictor20.toperguns;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;
import org.lmvictor20.toperguns.command.GiveWeaponCommand;
import org.lmvictor20.toperguns.listener.WeaponListener;
import org.lmvictor20.toperguns.weapon.WeaponManager;
import org.lmvictor20.toperguns.api.GunsAPI;
import org.lmvictor20.toperguns.api.impl.GunsAPIImpl;

public class ToperGunsPlugin extends JavaPlugin {

    private static ToperGunsPlugin instance;
    private WeaponManager weaponManager;

    public static ToperGunsPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("weapons.yml", false);

        this.weaponManager = new WeaponManager(this);
        this.weaponManager.reload();

        Bukkit.getPluginManager().registerEvents(new WeaponListener(this), this);

        // Register public API as a Bukkit service
        GunsAPI api = new GunsAPIImpl(this);
        Bukkit.getServicesManager().register(GunsAPI.class, api, this, ServicePriority.Normal);

        PluginCommand give = getCommand("giveweapon");
        if (give != null) {
            give.setExecutor(new GiveWeaponCommand(this));
        }
    }

    public WeaponManager getWeaponManager() {
        if (weaponManager == null) {
            weaponManager = new WeaponManager(this);
            weaponManager.reload();
        }
        return weaponManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("giveweapon")) {
            return new GiveWeaponCommand(this).onCommand(sender, cmd, label, args);
        }
        return false;
    }
}


